#!/bin/bash
ACTUATOR_URL="http://localhost:9090/actuator/metrics"

while true; do
  clear
  echo "===== $(date '+%H:%M:%S') ====="

  echo ""
  echo "[HikariCP]"
  ACTIVE=$(curl -s $ACTUATOR_URL/hikaricp.connections.active | jq -r '.measurements[0].value')
  IDLE=$(curl -s $ACTUATOR_URL/hikaricp.connections.idle | jq -r '.measurements[0].value')
  PENDING=$(curl -s $ACTUATOR_URL/hikaricp.connections.pending | jq -r '.measurements[0].value')
  MAX=$(curl -s $ACTUATOR_URL/hikaricp.connections.max | jq -r '.measurements[0].value')
  echo "  active: $ACTIVE / max: $MAX"
  echo "  idle: $IDLE | pending: $PENDING"

  echo ""
  echo "[Tomcat]"
  BUSY=$(curl -s $ACTUATOR_URL/tomcat.threads.busy | jq -r '.measurements[0].value')
  CURRENT=$(curl -s $ACTUATOR_URL/tomcat.threads.current | jq -r '.measurements[0].value')
  echo "  busy: $BUSY / current: $CURRENT"

  echo ""
  echo "[JVM Memory]"
  HEAP=$(curl -s "$ACTUATOR_URL/jvm.memory.used?tag=area:heap" | jq -r '.measurements[0].value')
  HEAP_MB=$(echo "scale=0; $HEAP / 1048576" | bc)
  echo "  heap: ${HEAP_MB}MB"

  sleep 2
done