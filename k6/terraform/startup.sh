#!/bin/bash
set -euo pipefail

# k6 설치
sudo gpg -k
sudo gpg --no-default-keyring \
  --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update -y
sudo apt-get install -y k6

# 테스트 파일 디렉토리 준비
mkdir -p /home/ubuntu/k6
chown ubuntu:ubuntu /home/ubuntu/k6

echo "k6 설치 완료. 테스트 파일을 scp로 복사하거나 git clone 후 실행하세요." \
  >> /home/ubuntu/k6/README.txt
