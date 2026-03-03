#!/bin/bash
# ===========================================
# TechFork 백업 초기 설정 스크립트 (서버 최초 1회 실행)
#
# 역할:
#   1. OCI CLI 설치 (Instance Principal 인증용)
#   2. ES 스냅샷 디렉토리 생성
#   3. OCI 인증 확인
#
# 백업 실행은 GitHub Actions (backup.yml)이 담당
#
# 실행 방법:
#   ssh ubuntu@<SERVER_IP>
#   bash ~/deploy/docker/backup/setup-cron.sh
# ===========================================
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

# ===========================================
# 1. OCI CLI 설치
# ===========================================
install_oci_cli() {
  if command -v oci > /dev/null 2>&1 || [ -x "/home/ubuntu/bin/oci" ]; then
    log "OCI CLI 이미 설치됨: $(oci --version 2>/dev/null || echo 'version unknown')"
    return
  fi

  log "OCI CLI 설치 중..."
  curl -fsSL https://raw.githubusercontent.com/oracle/oci-cli/master/scripts/install/install.sh \
    | bash -s -- --accept-all-defaults

  export PATH="$PATH:/home/ubuntu/bin"
  log "OCI CLI 설치 완료: $(oci --version)"
}

# ===========================================
# 2. ES 스냅샷 디렉토리 생성
# ===========================================
setup_es_snapshot_dir() {
  local snapshot_dir="/home/ubuntu/deploy/es-snapshots"

  mkdir -p "${snapshot_dir}"
  chmod 777 "${snapshot_dir}"  # ES 컨테이너(UID 1000)가 쓸 수 있도록
  log "ES 스냅샷 디렉토리 준비 완료: ${snapshot_dir}"
}

# ===========================================
# 3. OCI Instance Principal 인증 테스트
# ===========================================
test_oci_auth() {
  log "OCI Instance Principal 인증 테스트..."
  local oci_cli
  oci_cli=$(command -v oci 2>/dev/null || echo "/home/ubuntu/bin/oci")

  if ${oci_cli} os ns get --auth instance_principal > /dev/null 2>&1; then
    local ns
    ns=$(${oci_cli} os ns get --auth instance_principal --query 'data' --raw-output)
    log "OCI 인증 성공! 네임스페이스: ${ns}"
  else
    log "WARNING: OCI Instance Principal 인증 실패"
    log "  → terraform apply가 완료되었는지 확인하세요"
    log "  → Dynamic Group / IAM Policy 생성 후 최대 2분 소요됩니다"
  fi
}

# ===========================================
# 메인
# ===========================================
main() {
  log "========================================"
  log "TechFork 백업 초기 설정 시작"
  log "========================================"

  install_oci_cli
  setup_es_snapshot_dir
  test_oci_auth

  log "========================================"
  log "초기 설정 완료!"
  log ""
  log "다음 단계:"
  log "  1. Elasticsearch 재시작 (path.repo 설정 반영)"
  log "     cd ~/deploy && docker compose -f docker/docker-compose.infra.yml restart elasticsearch"
  log ""
  log "  2. 백업 수동 실행 테스트"
  log "     bash ~/deploy/docker/backup/backup.sh"
  log "     tail -f ~/deploy/backup.log"
  log ""
  log "  3. 이후 백업은 GitHub Actions (backup.yml)이 매일 02:00 KST에 자동 실행"
  log "========================================"
}

main "$@"
