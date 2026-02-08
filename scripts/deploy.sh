#!/bin/bash
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
STATE_FILE="${DEPLOY_DIR}/.active-color"
LOCK_DIR="${DEPLOY_DIR}/.deploy.lock"
DOCKER_DIR="${DEPLOY_DIR}/docker"
COMPOSE_INFRA="${DOCKER_DIR}/docker-compose.infra.yml"
COMPOSE_BLUE="${DOCKER_DIR}/docker-compose.blue.yml"
COMPOSE_GREEN="${DOCKER_DIR}/docker-compose.green.yml"
UPSTREAM_CONF="${DOCKER_DIR}/nginx/conf.d/upstream.conf"

HEALTH_CHECK_RETRIES=30
HEALTH_CHECK_INTERVAL=5

# ========== Helper Functions ==========

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

cleanup() {
    rmdir "$LOCK_DIR" 2>/dev/null || true
}

get_active_color() {
    if [ -f "$STATE_FILE" ]; then
        cat "$STATE_FILE"
    else
        echo ""
    fi
}

get_target_color() {
    if [ "$1" = "blue" ]; then
        echo "green"
    else
        echo "blue"
    fi
}

get_compose_file() {
    if [ "$1" = "blue" ]; then
        echo "$COMPOSE_BLUE"
    else
        echo "$COMPOSE_GREEN"
    fi
}

get_container_name() {
    echo "techfork-app-$1"
}

health_check() {
    local container="$1"

    log "Waiting for ${container} to become healthy..."
    for i in $(seq 1 "$HEALTH_CHECK_RETRIES"); do
        if docker exec techfork-nginx curl -sf "http://${container}:8080/actuator/health" > /dev/null 2>&1; then
            log "Health check passed (attempt ${i}/${HEALTH_CHECK_RETRIES})"
            return 0
        fi
        log "Health check attempt ${i}/${HEALTH_CHECK_RETRIES} failed, waiting ${HEALTH_CHECK_INTERVAL}s..."
        sleep "$HEALTH_CHECK_INTERVAL"
    done

    log "ERROR: Health check failed after ${HEALTH_CHECK_RETRIES} attempts"
    return 1
}

switch_upstream() {
    local container
    container=$(get_container_name "$1")

    log "Switching nginx upstream to ${container}..."
    cat > "$UPSTREAM_CONF" <<EOF
upstream springapp {
    server ${container}:8080 fail_timeout=0;
}
EOF

    if ! docker exec techfork-nginx nginx -t > /dev/null 2>&1; then
        log "ERROR: Nginx config test failed!"
        return 1
    fi

    docker exec techfork-nginx nginx -s reload
    log "Nginx reloaded successfully"
}

# ========== Main ==========

log "=== Blue-Green Deployment Started ==="

# Lock
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    log "ERROR: Another deployment is already running. Exiting."
    exit 1
fi
trap cleanup EXIT

# Generate .env from SSH-injected environment variables
log "Writing .env file..."
env | grep -E '^(DOCKER_IMAGE|BRANCH|SPRING_PROFILES_ACTIVE|DB_|REDIS_|ANTHROPIC_|OPENAI_|DISCORD_|KAKAO_|APPLE_|JWT_|SERVER_)' > "${DOCKER_DIR}/.env"
chmod 600 "${DOCKER_DIR}/.env"

# Step 1: Ensure Docker network exists
log "Ensuring Docker network exists..."
docker network create techfork-network 2>/dev/null || true

# Step 2: Start infrastructure
log "Starting infrastructure services..."
docker compose -f "$COMPOSE_INFRA" up -d

log "Waiting for Elasticsearch to be healthy..."
timeout 120 bash -c 'until docker exec techfork-elasticsearch curl -sf http://localhost:9200/_cluster/health > /dev/null 2>&1; do sleep 5; done' || {
    log "WARNING: Elasticsearch health check timed out, proceeding anyway..."
}

# Step 3: Determine colors
ACTIVE_COLOR=$(get_active_color)
if [ -z "$ACTIVE_COLOR" ]; then
    log "No active color found (first deployment). Deploying blue."
    TARGET_COLOR="blue"
else
    TARGET_COLOR=$(get_target_color "$ACTIVE_COLOR")
    log "Active: ${ACTIVE_COLOR}, Target: ${TARGET_COLOR}"
fi

TARGET_COMPOSE=$(get_compose_file "$TARGET_COLOR")
TARGET_CONTAINER=$(get_container_name "$TARGET_COLOR")

# Step 4: Pull new image
log "Pulling latest image for ${TARGET_COLOR}..."
docker compose -f "$TARGET_COMPOSE" pull

# Step 5: Start target container
log "Starting ${TARGET_COLOR} container..."
docker compose -f "$TARGET_COMPOSE" up -d

# Step 6: Health check
if health_check "$TARGET_CONTAINER"; then
    log "Target container ${TARGET_CONTAINER} is healthy"
else
    log "ROLLBACK: Stopping failed ${TARGET_COLOR} container..."
    docker compose -f "$TARGET_COMPOSE" down
    log "Rollback complete. Active deployment unchanged: ${ACTIVE_COLOR:-none}"
    exit 1
fi

# Step 7: Switch nginx upstream
switch_upstream "$TARGET_COLOR"

# Step 8: Grace period for in-flight requests
sleep 10

# Step 9: Stop old container
if [ -n "$ACTIVE_COLOR" ]; then
    OLD_COMPOSE=$(get_compose_file "$ACTIVE_COLOR")
    log "Stopping old ${ACTIVE_COLOR} container..."
    docker compose -f "$OLD_COMPOSE" down
fi

# Step 10: Save active color
echo "$TARGET_COLOR" > "$STATE_FILE"
log "Active color is now: ${TARGET_COLOR}"

# Step 11: Cleanup
log "Pruning unused Docker images..."
docker image prune -af

log "=== Blue-Green Deployment Completed Successfully ==="
