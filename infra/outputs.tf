output "ec2_public_ip" {
  description = "EC2 퍼블릭 IP"
  content       = aws_instance.app.public_ip
}

output "ec2_public_dns" {
  description = "EC2 퍼블릭 DNS"
  content       = aws_instance.app.public_dns
}

output "rds_endpoint" {
  description = "RDS 엔드포인트"
  content       = aws_db_instance.main.endpoint
}

output "connection_info" {
  description = "연결 정보"
  sensitive   = true
  content = <<-EOT
    
    ===== Tech Blog 인프라 정보 =====
    
    EC2 서버 SSH 접속:
      ssh ec2-user@${aws_instance.app.public_ip}
    
    애플리케이션 URL:
      http://${aws_instance.app.public_ip}:8080
    
    Redis (로컬 접속):
      Host: localhost (127.0.0.1)
      Port: 6379
    
    RDS MySQL:
      Host: ${aws_db_instance.main.endpoint}
      Database: techblog
      Username: ${var.db_username}
    
    ===================================
  EOT
}