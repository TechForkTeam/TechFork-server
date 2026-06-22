# PROJECT KNOWLEDGE BASE

**Generated:** 2026-03-23 Asia/Seoul
**Commit:** 4e15526
**Branch:** improve/#338

## OVERVIEW
TechFork is a Spring Boot 3.5.9 / Java 17 backend that crawls Korean tech blogs, stores posts in MySQL, enriches them with LLM summaries and embeddings, and serves search/recommendation APIs over Elasticsearch.

## STRUCTURE
```text
./
├── docs/                 # scheduler, crawl-pipeline, commit/PR conventions
├── docker/               # local/dev/infra/blue-green compose + nginx + backups
├── infra/                # Terraform stacks; committed tfstate/tfvars are high-risk
├── k6/                   # load scenarios + GCP runner Terraform
├── scripts/              # deploy, tunnel, monitor helpers
├── src/main/java/com/techfork/domain/   # bounded business domains
├── src/main/java/com/techfork/global/   # shared response/error/security/LLM infra
└── src/test/java/com/techfork/          # integration + evaluation suites
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| App bootstrap | `src/main/java/com/techfork/TechForkApplication.java` | `@SpringBootApplication`, `@EnableJpaAuditing` |
| Crawl pipeline | `src/main/java/com/techfork/domain/source/` | Child AGENTS covers job/scheduler invariants |
| Initial seed data | `src/main/java/com/techfork/global/config/InitialDataConfig.java` | Seeds tech blogs in `local`, `local-tunnel`, `dev` |
| Response / errors | `src/main/java/com/techfork/global/response/`, `global/exception/`, `global/common/code/` | `BaseResponse.of(...)`, `BaseCode`, `GeneralException` |
| Security / auth | `src/main/java/com/techfork/auth/` | Auth / Security owns JWT/OAuth/filter/cookie/cache under `auth/security` |
| Search hot path | `src/main/java/com/techfork/domain/search/` | `SearchServiceImpl` is one of the largest main-code files |
| Deployment topology | `docker/`, `scripts/deploy.sh`, `.github/workflows/cd.yml` | Blue-green + nginx upstream switching |
| Infra provisioning | `infra/` | AWS + Oracle stacks, committed state artifacts |
| Evaluation workflow | `src/test/java/com/techfork/evaluation/` | Child AGENTS covers tags, profiles, fixtures |
| Load testing | `k6/` | Child AGENTS covers env contract and scenario layout |

## CONVENTIONS
- Default runtime profile is `local-tunnel`; `application.yml` imports `.env` directly.
- Controllers return `BaseResponse.of(code[, data])`; raw controller bodies are non-standard here.
- Domain packages follow bounded contexts; newer slices use top-level context packages such as `auth`, `activity`, `post`, `useraccount`, and `personalization`, while legacy slices may still live under `domain/<context>`.
- Entities use protected no-args constructors plus static `create(...)` factories.
- Spring Batch schema is managed via Flyway files under `src/main/resources/db/migration/`, not auto-init in production-style profiles.
- Test execution is tag-split in Gradle: `test`, `integrationTest`, `evaluationTest`, `evaluationSetup`.
- Source ingestion orchestration lives in `domain/source`, but summary / embedding batch artifacts live in `domain/post/batch`.

## ANTI-PATTERNS (THIS PROJECT)
- Never commit or casually edit `.env`, `keys/`, `infra/terraform.tfstate`, `infra/*.tfvars`.
- Do not treat `auth/application` or `auth/presentation` as the full auth surface; JWT, OAuth handlers, filters, cookies, and auth cache live under `auth/security`.
- Do not duplicate `CLAUDE.md` or `docs/source-package.md` into child AGENTS files; summarize and point.
- Do not edit only one of `docker-compose.blue.yml` / `docker-compose.green.yml` unless the asymmetry is intentional.
- Do not run evaluation-heavy suites as if they were ordinary integration tests; they have separate tags, profiles, fixtures, and runtime cost.
- Do not trust docs over code when they disagree; example: scheduler docs mention hourly behavior, but `RssCrawlingScheduler` currently runs daily at 05:00 KST.

## UNIQUE STYLES
- Commit format: `<type>: <subject>` (`docs/convention/commit-convention.md`).
- PR title format: `[type/#issue] description` (`docs/convention/pr-convention.md`).
- Korean messages/comments are normal in code and docs.
- Evaluation outputs are checked into `src/test/resources/` as JSON reports.
- Operational knowledge is split across docs, compose files, shell scripts, and workflows rather than a single ops README.

## COMMANDS
```bash
./gradlew build
./gradlew test
./gradlew integrationTest
./gradlew evaluationTest
./gradlew evaluationSetup
./gradlew bootRun --args='--spring.profiles.active=local'
docker compose -f docker/docker-compose.local.yml up -d
```

## NOTES
- High-value docs: `docs/source-package.md`, `docs/SCHEDULER_GUIDE.md`, `docs/convention/commit-convention.md`, `docs/convention/pr-convention.md`.
- `HELP.md` is Spring starter boilerplate; low signal compared with repo docs.
- Child AGENTS live at:
  - `src/main/java/com/techfork/domain/source/AGENTS.md`
  - `src/test/java/com/techfork/evaluation/AGENTS.md`
  - `docker/AGENTS.md`
  - `infra/AGENTS.md`
  - `k6/AGENTS.md`
