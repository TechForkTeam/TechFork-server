#!/bin/bash
set -e

# 로그 설정
exec > >(tee /var/log/cloud-init-custom.log) 2>&1
echo "===== Cloud Init Started: $(date) ====="

# ===========================================
# 시스템 업데이트
# ===========================================
echo "===== System Update ====="
apt-get update -y
apt-get upgrade -y

# ===========================================
# Swap 메모리 설정 (4GB - ES 안정성을 위해)
# ===========================================
echo "===== Swap Memory Setup (4GB) ====="
if [ ! -f /swapfile ]; then
    fallocate -l 4G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    
    # Swappiness 설정 (ES 권장: 낮은 값)
    echo 'vm.swappiness=1' >> /etc/sysctl.conf
    sysctl -p
    
    echo "Swap setup complete!"
    free -h
fi

# ===========================================
# Docker 설치 (ARM64)
# ===========================================
echo "===== Install Docker ====="
apt-get install -y ca-certificates curl gnupg lsb-release

# Docker GPG 키 추가
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

# Docker 저장소 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Docker 서비스 시작
systemctl start docker
systemctl enable docker

# ubuntu 사용자를 docker 그룹에 추가
usermod -aG docker ubuntu

echo "Docker version: $(docker --version)"

# ===========================================
# Nginx 설치
# ===========================================
echo "===== Install Nginx ====="
apt-get install -y nginx

# ===========================================
# 애플리케이션 디렉토리 생성
# ===========================================
echo "===== Create Application Directories ====="
mkdir -p /opt/tech-fork
mkdir -p /var/log/tech-fork
chown -R ubuntu:ubuntu /opt/tech-fork
chown -R ubuntu:ubuntu /var/log/tech-fork

# ===========================================
# 시스템 튜닝 (Elasticsearch 권장 설정)
# ===========================================
echo "===== System Tuning for Elasticsearch ====="

# vm.max_map_count (ES 필수)
echo 'vm.max_map_count=262144' >> /etc/sysctl.conf
sysctl -w vm.max_map_count=262144

# 파일 디스크립터 제한 증가
cat >> /etc/security/limits.conf <<EOF
* soft nofile 65536
* hard nofile 65536
* soft nproc 65536
* hard nproc 65536
ubuntu soft memlock unlimited
ubuntu hard memlock unlimited
EOF

# ===========================================
# Nginx 설정
# ===========================================
echo "===== Configure Nginx ====="
cat > /etc/nginx/sites-available/tech-fork <<'NGINX'
upstream springapp {
    server 127.0.0.1:8080 fail_timeout=0;
}

server {
    listen 80;
    server_name ${domain_name} www.${domain_name} api.${domain_name};

    client_max_body_size 10M;

    access_log /var/log/nginx/tech-fork-access.log;
    error_log /var/log/nginx/tech-fork-error.log;

    # Cloudflare Real IP 설정
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
        proxy_pass http://springapp;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 타임아웃 설정 (검색 응답 시간 고려)
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health Check 엔드포인트
    location /health {
        proxy_pass http://springapp/actuator/health;
        proxy_set_header Host $host;
    }
}
NGINX

# 사이트 활성화
ln -sf /etc/nginx/sites-available/tech-fork /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Nginx 테스트 및 재시작
nginx -t
systemctl restart nginx
systemctl enable nginx

# ===========================================
# Docker Compose 파일 생성 (Oracle 최적화)
# ===========================================
echo "===== Create Docker Compose File ====="
cat > /opt/tech-fork/docker-compose.yml <<'COMPOSE'
version: '3.8'

services:
  app:
    image: $${DOCKER_IMAGE}:$${BRANCH}
    container_name: tech-fork-app
    restart: always
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=$${SPRING_PROFILES_ACTIVE}
      - DB_URL=$${DB_URL}
      - DB_USERNAME=$${DB_USERNAME}
      - DB_PASSWORD=$${DB_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=$${REDIS_PASSWORD}
      - ELASTICSEARCH_HOST=elasticsearch
      - ELASTICSEARCH_PORT=9200
      - ANTHROPIC_API_KEY=$${ANTHROPIC_API_KEY}
      - OPENAI_API_KEY=$${OPENAI_API_KEY}
    networks:
      - app-network
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
      elasticsearch:
        condition: service_healthy

  mysql:
    image: mysql:8.0
    container_name: tech-fork-mysql
    restart: always
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=$${DB_PASSWORD}
      - MYSQL_DATABASE=techblog
      - MYSQL_USER=techfork
      - MYSQL_PASSWORD=$${DB_PASSWORD}
      - TZ=Asia/Seoul
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --innodb-buffer-pool-size=2G
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: tech-fork-redis
    restart: always
    ports:
      - "6379:6379"
    command: >
      redis-server
      --requirepass $${REDIS_PASSWORD}
      --maxmemory 1gb
      --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    networks:
      - app-network

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.18.0
    container_name: tech-fork-elasticsearch
    restart: always
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms8g -Xmx8g"
      - cluster.routing.allocation.disk.threshold_enabled=true
      - cluster.routing.allocation.disk.watermark.low=85%
      - cluster.routing.allocation.disk.watermark.high=90%
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    networks:
      - app-network
    ulimits:
      memlock:
        soft: -1
        hard: -1
      nofile:
        soft: 65536
        hard: 65536
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 120s

networks:
  app-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
  elasticsearch-data:
COMPOSE

chown -R ubuntu:ubuntu /opt/tech-fork

# ===========================================
# 환경 변수 템플릿 생성
# ===========================================
echo "===== Create Environment Template ====="
cat > /opt/tech-fork/.env.template <<ENV
DOCKER_IMAGE=ghcr.io/your-org/tech-fork
BRANCH=main
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://mysql:3306/techblog
DB_USERNAME=techfork
DB_PASSWORD=${db_password}
REDIS_PASSWORD=${redis_password}
ANTHROPIC_API_KEY=your-anthropic-key
OPENAI_API_KEY=your-openai-key
ENV

# ===========================================
# 방화벽 설정 (iptables)
# ===========================================
echo "===== Configure Firewall ====="
# Oracle Cloud는 Security List에서 관리하므로 iptables는 기본 허용
iptables -F
iptables -P INPUT ACCEPT
iptables -P FORWARD ACCEPT
iptables -P OUTPUT ACCEPT

# ===========================================
# 완료
# ===========================================
echo "===== Cloud Init Completed: $(date) ====="
echo "Next steps:"
echo "1. SSH into the instance: ssh ubuntu@<public-ip>"
echo "2. Copy .env.template to .env and fill in the values"
echo "3. Run: cd /opt/tech-fork && docker compose up -d"
