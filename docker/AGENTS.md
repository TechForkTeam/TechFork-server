# DOCKER TOPOLOGY GUIDE

## OVERVIEW
`docker/` defines four different runtime modes: standalone local dev, shared infra, blue-green app deploy, and a separate dev app container. It also owns nginx routing and backup scripts.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Local stack | `docker-compose.local.yml` | MySQL, Redis, ES, Kibana with local volumes |
| Shared infra stack | `docker-compose.infra.yml` | External network + external volumes |
| Production slots | `docker-compose.blue.yml`, `docker-compose.green.yml` | Blue on `8080`, green on `8081` |
| Dev slot | `docker-compose.dev.yml` | `techfork-app-dev` on shared network |
| Upstream switch contract | `nginx/conf.d/upstream.conf` | Rewritten by `scripts/deploy.sh` |
| Backup flow | `backup/backup.sh` | MySQL + ES snapshots uploaded to OCI |

## CONVENTIONS
- `docker-compose.infra.yml` expects external network `techfork-network` and external volumes named `deploy_*`.
- Blue and green compose files should stay structurally symmetric; the intended delta is slot identity/host port, not behavior drift.
- Health checks use `/actuator/health` on port `9090`; deploy logic depends on that exact contract.
- Infra ES mounts `/home/ubuntu/deploy/es-snapshots` for snapshot backups; this is part of backup, not optional debug state.
- `backup.sh` loads secrets from `/home/ubuntu/deploy/docker/.env` and uploads to OCI Object Storage, not AWS.
- Nginx config is coupled to Docker naming (`techfork-app-blue`, `techfork-app-green`, `techfork-app-dev`, `techfork-nginx`).

## ANTI-PATTERNS
- Do not edit only one of blue/green unless the asymmetry is deliberate and documented.
- Do not rename `techfork-network`, container names, or upstream targets without updating `scripts/deploy.sh` and nginx config together.
- Do not hardcode secrets into compose files; the env contract is already large enough.
- Do not treat local compose behavior as equivalent to infra/prod; local uses different networking and resource sizing.

## NOTES
- Redis is intentionally hardened in infra mode by renaming `KEYS`, `FLUSHALL`, and `FLUSHDB`.
- Infra ES runs with `-Xms8g -Xmx8g`; local ES runs much smaller.
- If a change touches compose, nginx, and deploy script together, review `scripts/deploy.sh` before editing.
