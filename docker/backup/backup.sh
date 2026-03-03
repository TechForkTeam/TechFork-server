#!/bin/bash
# ===========================================
# TechFork 데이터베이스 백업 스크립트
#
# 대상: MySQL(mysqldump), Elasticsearch(Snapshot API)
# 저장소: OCI Object Storage (Instance Principal 인증)
# 실행: 크론잡 (매일 02:00 KST = 17:00 UTC)
#
# 디렉토리 구조 (버킷 내부):
#   mysql/mysql_YYYYMMDD_HHMMSS.sql.gz
#   elasticsearch/elasticsearch_YYYYMMDD_HHMMSS.tar.gz
# ===========================================
set -euo pipefail

# ===========================================
# 환경 변수 로드 (.env 파일에서 DB_PASSWORD 등)
# ===========================================
ENV_FILE="/home/ubuntu/deploy/docker/.env"
if [ -f "${ENV_FILE}" ]; then
  set -a
  # shellcheck source=/dev/null
  source "${ENV_FILE}"
  set +a
fi

# ===========================================
# 설정
# ===========================================
DATE=$(date +%Y%m%d_%H%M%S)
TEMP_DIR=$(mktemp -d)
LOG_FILE="/var/log/techfork-backup.log"

MYSQL_CONTAINER="techfork-mysql"
DB_ROOT_PASSWORD="${DB_PASSWORD:-}"

ES_HOST="http://localhost:9200"
ES_REPO_NAME="techfork_backup"
ES_SNAPSHOT_HOST_DIR="/home/ubuntu/deploy/es-snapshots"

OCI_BUCKET="${PROJECT_NAME:-tech-fork}-${ENVIRONMENT:-prod}-backups"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

# OCI CLI 경로 (기본 설치 위치 또는 시스템 PATH)
OCI_CLI=$(command -v oci 2>/dev/null || echo "/home/ubuntu/bin/oci")

# ===========================================
# 로깅 함수
# ===========================================
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "${LOG_FILE}"
}

error_exit() {
  log "ERROR: $*"
  exit 1
}

# ===========================================
# 사전 검증
# ===========================================
check_prerequisites() {
  if [ ! -x "${OCI_CLI}" ]; then
    error_exit "OCI CLI를 찾을 수 없습니다. setup-cron.sh를 먼저 실행하세요."
  fi

  if ! docker info > /dev/null 2>&1; then
    error_exit "Docker가 실행 중이지 않습니다."
  fi

  # OCI Instance Principal 인증 확인
  OCI_NAMESPACE=$(${OCI_CLI} os ns get --auth instance_principal --query 'data' --raw-output 2>/dev/null) \
    || error_exit "OCI Instance Principal 인증 실패. Dynamic Group 및 IAM Policy 설정을 확인하세요."

  log "OCI 네임스페이스: ${OCI_NAMESPACE}"
}

# ===========================================
# OCI Object Storage 업로드
# ===========================================
upload_to_oci() {
  local local_file="$1"
  local remote_path="$2"

  log "OCI 업로드 중: ${OCI_BUCKET}/${remote_path}"
  ${OCI_CLI} os object put \
    --auth instance_principal \
    --namespace "${OCI_NAMESPACE}" \
    --bucket-name "${OCI_BUCKET}" \
    --name "${remote_path}" \
    --file "${local_file}" \
    --force
  log "OCI 업로드 완료"
}

# ===========================================
# MySQL 백업
# mysqldump → gzip → OCI Object Storage
# ===========================================
backup_mysql() {
  log "===== MySQL 백업 시작 ====="
  local backup_file="${TEMP_DIR}/mysql_${DATE}.sql.gz"

  if ! docker ps --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
    error_exit "MySQL 컨테이너(${MYSQL_CONTAINER})가 실행 중이지 않습니다."
  fi

  if [ -z "${DB_ROOT_PASSWORD}" ]; then
    error_exit "DB_PASSWORD 환경변수가 설정되지 않았습니다. ${ENV_FILE} 파일을 확인하세요."
  fi

  log "mysqldump 실행 중..."
  docker exec "${MYSQL_CONTAINER}" \
    mysqldump \
      -u root \
      -p"${DB_ROOT_PASSWORD}" \
      --single-transaction \
      --routines \
      --triggers \
      --all-databases \
      2>/dev/null \
    | gzip > "${backup_file}"

  local size
  size=$(du -sh "${backup_file}" | cut -f1)
  log "MySQL 백업 파일 생성 완료: ${size}"

  upload_to_oci "${backup_file}" "mysql/mysql_${DATE}.sql.gz"
  log "===== MySQL 백업 완료 ====="
}

# ===========================================
# Elasticsearch 백업
# Snapshot API → tar.gz → OCI Object Storage
# ===========================================
backup_elasticsearch() {
  log "===== Elasticsearch 백업 시작 ====="
  local backup_file="${TEMP_DIR}/elasticsearch_${DATE}.tar.gz"
  local snap_name="snapshot_${DATE}"

  # ES 상태 확인
  if ! curl -sf "${ES_HOST}/_cluster/health" > /dev/null 2>&1; then
    error_exit "Elasticsearch가 응답하지 않습니다."
  fi

  # 스냅샷 호스트 디렉토리 확인
  if [ ! -d "${ES_SNAPSHOT_HOST_DIR}" ]; then
    error_exit "ES 스냅샷 디렉토리가 없습니다: ${ES_SNAPSHOT_HOST_DIR}. setup-cron.sh를 먼저 실행하세요."
  fi

  # 스냅샷 저장소 등록 (없으면 생성, 있으면 덮어쓰기)
  log "Elasticsearch 스냅샷 저장소 설정..."
  curl -sf -X PUT "${ES_HOST}/_snapshot/${ES_REPO_NAME}" \
    -H 'Content-Type: application/json' \
    -d '{
      "type": "fs",
      "settings": {
        "location": "/usr/share/elasticsearch/snapshots",
        "compress": true
      }
    }' > /dev/null

  # 동일 이름 스냅샷 사전 정리
  curl -sf -X DELETE "${ES_HOST}/_snapshot/${ES_REPO_NAME}/${snap_name}" \
    > /dev/null 2>&1 || true

  # 스냅샷 생성 (완료까지 동기 대기)
  log "Elasticsearch 스냅샷 생성 중 (완료까지 대기)..."
  local response
  response=$(curl -sf -X PUT \
    "${ES_HOST}/_snapshot/${ES_REPO_NAME}/${snap_name}?wait_for_completion=true" \
    -H 'Content-Type: application/json' \
    -d '{"indices": "*", "ignore_unavailable": true, "include_global_state": false}')

  if ! echo "${response}" | grep -q '"state":"SUCCESS"'; then
    error_exit "스냅샷 생성 실패: ${response}"
  fi
  log "스냅샷 생성 완료"

  # 스냅샷 파일 압축
  tar czf "${backup_file}" -C "${ES_SNAPSHOT_HOST_DIR}" .

  local size
  size=$(du -sh "${backup_file}" | cut -f1)
  log "Elasticsearch 백업 파일 생성 완료: ${size}"

  upload_to_oci "${backup_file}" "elasticsearch/elasticsearch_${DATE}.tar.gz"

  # 로컬 스냅샷 파일 정리 (OCI 업로드 후 불필요)
  rm -rf "${ES_SNAPSHOT_HOST_DIR:?}"/*
  log "===== Elasticsearch 백업 완료 ====="
}

# ===========================================
# 오래된 백업 삭제 (OCI Lifecycle Policy 대신 직접 처리)
# OCI API의 time-created 기준으로 N일 이상된 객체 삭제
# ===========================================
cleanup_old_backups() {
  local prefix="$1"
  log "오래된 ${prefix}/ 백업 정리 중 (${BACKUP_RETENTION_DAYS}일 초과)..."

  local old_objects
  old_objects=$(${OCI_CLI} os object list \
    --auth instance_principal \
    --namespace "${OCI_NAMESPACE}" \
    --bucket-name "${OCI_BUCKET}" \
    --prefix "${prefix}/" \
    --all \
    --output json 2>/dev/null \
    | python3 -c "
import sys, json
from datetime import datetime, timedelta, timezone

data = json.load(sys.stdin)
cutoff = datetime.now(timezone.utc) - timedelta(days=${BACKUP_RETENTION_DAYS})

for obj in data.get('data', []):
    try:
        created = datetime.fromisoformat(obj['time-created'])
        if created < cutoff:
            print(obj['name'])
    except Exception:
        pass
")

  if [ -z "${old_objects}" ]; then
    log "삭제할 오래된 백업 없음"
    return
  fi

  echo "${old_objects}" | while read -r obj_name; do
    [ -z "${obj_name}" ] && continue
    log "삭제: ${obj_name}"
    ${OCI_CLI} os object delete \
      --auth instance_principal \
      --namespace "${OCI_NAMESPACE}" \
      --bucket-name "${OCI_BUCKET}" \
      --object-name "${obj_name}" \
      --force
  done
  log "${prefix}/ 정리 완료"
}

# ===========================================
# 임시 파일 정리 (EXIT 시 자동 실행)
# ===========================================
cleanup() {
  rm -rf "${TEMP_DIR}"
}

# ===========================================
# 메인
# ===========================================
main() {
  log "========================================"
  log "TechFork 백업 시작"
  log "========================================"

  trap cleanup EXIT

  check_prerequisites
  backup_mysql
  backup_elasticsearch
  cleanup_old_backups "mysql"
  cleanup_old_backups "elasticsearch"

  log "========================================"
  log "TechFork 백업 완료"
  log "========================================"
}

main "$@"
