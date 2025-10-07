# VPC
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.project_name}-${var.environment}-vpc"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-${var.environment}-igw"
  }
}

# Public Subnets
resource "aws_subnet" "public" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 1}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]
  
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-${var.environment}-public-subnet-${count.index + 1}"
  }
}

# Private Subnets (for RDS)
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 10}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "${var.project_name}-${var.environment}-private-subnet-${count.index + 1}"
  }
}

# Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Availability Zones Data
data "aws_availability_zones" "available" {
  state = "available"
}

# Latest Amazon Linux 2 AMI
data "aws_ami" "amazon_linux_2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}

# EC2 Security Group
resource "aws_security_group" "ec2" {
  name        = "${var.project_name}-${var.environment}-ec2-sg"
  description = "Security group for EC2 instance"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Spring Boot"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-ec2-sg"
  }
}

# RDS Security Group
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-${var.environment}-rds-sg"
  description = "Security group for RDS instance"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "MySQL from EC2"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }

  ingress {
    description = "MySQL from anywhere (development)"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-rds-sg"
  }
}

# IAM Role for EC2
resource "aws_iam_role" "ec2_role" {
  name = "${var.project_name}-${var.environment}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "${var.project_name}-${var.environment}-ec2-role"
  }
}

# CloudWatch Agent Policy Attachment
resource "aws_iam_role_policy_attachment" "ec2_cloudwatch" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# SSM Policy Attachment
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# EC2 Instance Profile
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.project_name}-${var.environment}-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# EC2 Instance (Spring Boot + Nginx)
resource "aws_instance" "app" {
  ami                    = data.aws_ami.amazon_linux_2.id
  instance_type          = var.ec2_instance_type
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = <<-EOF
              #!/bin/bash
              
              # Log configuration
              exec > >(tee /var/log/user-data.log)
              exec 2>&1
              
              echo "===== System Update Start ====="
              yum update -y

              echo "===== Swap Memory Setup (2GB) ====="
              if [ ! -f /swapfile ]; then
                dd if=/dev/zero of=/swapfile bs=128M count=16
                chmod 600 /swapfile
                mkswap /swapfile
                swapon /swapfile
                echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
                echo 'vm.swappiness=10' >> /etc/sysctl.conf
                sysctl -p
                echo "Swap memory setup complete!"
                swapon --show
                free -h
              else
                echo "Swap file already exists."
              fi
              
              echo "===== Install Java 17 ====="
              yum install -y java-17-amazon-corretto-devel
              
              echo "===== Install Nginx ====="
              amazon-linux-extras install -y nginx1
              
              echo "===== Create Application Directories ====="
              mkdir -p /opt/tech-blog
              mkdir -p /var/log/tech-blog
              
              useradd -r -s /bin/false tech-blog || true
              chown -R tech-blog:tech-blog /opt/tech-blog
              chown -R tech-blog:tech-blog /var/log/tech-blog
              
              echo "===== Install Git ====="
              yum install -y git
              
              echo "===== Nginx Configuration ====="
              cat > /etc/nginx/conf.d/tech-blog.conf <<'NGINX'
              upstream spring_backend {
                  server 127.0.0.1:8080 fail_timeout=0;
              }

              server {
                  listen 80;
                  server_name ${var.domain_name} www.${var.domain_name};
                  
                  client_max_body_size 10M;
                  
                  access_log /var/log/nginx/tech-blog-access.log;
                  error_log /var/log/nginx/tech-blog-error.log;
                  
                  # Cloudflare Real IP
                  set_real_ip_from 173.245.48.0/20;
                  set_real_ip_from 103.21.244.0/22;
                  set_real_ip_from 103.22.200.0/22;
                  set_real_ip_from 103.31.4.0/22;
                  set_real_ip_from 141.101.64.0/18;
                  set_real_ip_from 108.162.192.0/18;
                  set_real_ip_from 190.93.240.0/20;
                  set_real_ip_from 188.114.96.0/20;
                  set_real_ip_from 197.234.240.0/22;
                  set_real_ip_from 198.41.128.0/17;
                  set_real_ip_from 162.158.0.0/15;
                  set_real_ip_from 104.16.0.0/13;
                  set_real_ip_from 104.24.0.0/14;
                  set_real_ip_from 172.64.0.0/13;
                  set_real_ip_from 131.0.72.0/22;
                  real_ip_header CF-Connecting-IP;
                  
                  location / {
                      proxy_pass http://spring_backend;
                      proxy_set_header Host \$host;
                      proxy_set_header X-Real-IP \$remote_addr;
                      proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
                      proxy_set_header X-Forwarded-Proto \$scheme;
                      
                      proxy_connect_timeout 60s;
                      proxy_send_timeout 60s;
                      proxy_read_timeout 60s;
                      
                      proxy_buffering on;
                      proxy_buffer_size 4k;
                      proxy_buffers 8 4k;
                  }
                  
                  location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2|ttf)$ {
                      proxy_pass http://spring_backend;
                      expires 30d;
                      add_header Cache-Control "public, immutable";
                  }
                  
                  location /health {
                      proxy_pass http://spring_backend/actuator/health;
                      access_log off;
                  }
              }
              NGINX
              
              systemctl start nginx
              systemctl enable nginx
              
              echo "===== Install CloudWatch Agent ====="
              wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
              rpm -U ./amazon-cloudwatch-agent.rpm
              
              cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json <<'CONFIG'
              {
                "metrics": {
                  "namespace": "TechBlog",
                  "metrics_collected": {
                    "mem": {
                      "measurement": [
                        {
                          "name": "mem_used_percent",
                          "rename": "MemoryUtilization",
                          "unit": "Percent"
                        }
                      ],
                      "metrics_collection_interval": 60
                    },
                    "disk": {
                      "measurement": [
                        {
                          "name": "used_percent",
                          "rename": "DiskUtilization",
                          "unit": "Percent"
                        }
                      ],
                      "metrics_collection_interval": 60,
                      "resources": [
                        "/"
                      ]
                    }
                  }
                },
                "logs": {
                  "logs_collected": {
                    "files": {
                      "collect_list": [
                        {
                          "file_path": "/var/log/tech-blog/*.log",
                          "log_group_name": "/aws/ec2/${var.project_name}-${var.environment}/application",
                          "log_stream_name": "{instance_id}",
                          "timezone": "Asia/Seoul"
                        },
                        {
                          "file_path": "/var/log/redis/redis.log",
                          "log_group_name": "/aws/ec2/${var.project_name}-${var.environment}/redis",
                          "log_stream_name": "{instance_id}",
                          "timezone": "Asia/Seoul"
                        },
                        {
                          "file_path": "/var/log/nginx/tech-blog-*.log",
                          "log_group_name": "/aws/ec2/${var.project_name}-${var.environment}/nginx",
                          "log_stream_name": "{instance_id}",
                          "timezone": "Asia/Seoul"
                        }
                      ]
                    }
                  }
                }
              }
              CONFIG
              
              /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
                -a fetch-config \
                -m ec2 \
                -s \
                -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
              
              echo "===== Installation Complete ====="
              echo "Nginx Status: $(systemctl is-active nginx)"
              echo "Java Version: $(java -version 2>&1 | head -n 1)"
              
              touch /var/log/user-data-complete.txt
              EOF

  tags = {
    Name = "${var.project_name}-${var.environment}-app-server"
  }
}

# RDS Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet-group"
  subnet_ids = aws_subnet.public[*].id

  tags = {
    Name = "${var.project_name}-${var.environment}-db-subnet-group"
  }
}

# RDS Instance
resource "aws_db_instance" "main" {
  identifier             = "${var.project_name}-${var.environment}-db"
  engine                 = "mysql"
  engine_version         = "8.0"
  instance_class         = var.rds_instance_class
  allocated_storage      = 20
  storage_type           = "gp3"
  
  db_name  = "techblog"
  username = var.db_username
  password = var.db_password
  
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  
  skip_final_snapshot       = true
  publicly_accessible       = true
  backup_retention_period   = 7
  enabled_cloudwatch_logs_exports = ["error", "general", "slowquery"]

  tags = {
    Name = "${var.project_name}-${var.environment}-rds"
  }
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "app_logs" {
  name              = "/aws/ec2/${var.project_name}-${var.environment}/application"
  retention_in_days = 7

  tags = {
    Name = "${var.project_name}-${var.environment}-app-logs"
  }
}

resource "aws_cloudwatch_log_group" "redis_logs" {
  name              = "/aws/ec2/${var.project_name}-${var.environment}/redis"
  retention_in_days = 7

  tags = {
    Name = "${var.project_name}-${var.environment}-redis-logs"
  }
}

resource "aws_cloudwatch_log_group" "nginx_logs" {
  name              = "/aws/ec2/${var.project_name}-${var.environment}/nginx"
  retention_in_days = 7

  tags = {
    Name = "${var.project_name}-${var.environment}-nginx-logs"
  }
}

resource "aws_cloudwatch_log_group" "rds_logs" {
  name              = "/aws/rds/${var.project_name}-${var.environment}/mysql"
  retention_in_days = 7

  tags = {
    Name = "${var.project_name}-${var.environment}-rds-logs"
  }
}

# SNS Topic for Alerts
resource "aws_sns_topic" "alerts" {
  name = "${var.project_name}-${var.environment}-alerts"

  tags = {
    Name = "${var.project_name}-${var.environment}-alerts"
  }
}

# SNS Email Subscription
resource "aws_sns_topic_subscription" "email_alerts" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# CloudWatch Alarms
resource "aws_cloudwatch_metric_alarm" "ec2_cpu_high" {
  alarm_name          = "${var.project_name}-${var.environment}-ec2-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "EC2 CPU utilization is over 80%"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.app.id
  }
}

resource "aws_cloudwatch_metric_alarm" "ec2_memory_high" {
  alarm_name          = "${var.project_name}-${var.environment}-ec2-memory-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "MemoryUtilization"
  namespace           = "TechBlog"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "EC2 memory utilization is over 80%"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.app.id
  }
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name          = "${var.project_name}-${var.environment}-rds-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "RDS CPU utilization is over 80%"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.id
  }
}