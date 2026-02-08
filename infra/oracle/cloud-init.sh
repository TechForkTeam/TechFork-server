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
# 시스템 튜닝 (Elasticsearch 권장 설정)
# ===========================================
echo "===== System Tuning for Elasticsearch ====="

# vm.max_map_count (ES 필수)
echo 'vm.max_map_count=262144' >> /etc/sysctl.conf
sysctl -w vm.max_map_count=262144

# ===========================================
# 배포 디렉토리 및 Docker 네트워크 생성
# ===========================================
echo "===== Setup Deployment Structure ====="
mkdir -p /home/ubuntu/deploy/nginx/conf.d
docker network create techfork-network 2>/dev/null || true
chown -R ubuntu:ubuntu /home/ubuntu/deploy

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
echo "2. Push to develop/main branch to trigger CD pipeline"