# K6 LOAD TEST GUIDE

## OVERVIEW
`k6/` contains scenario scripts, aggregate load plans, and a separate Google Cloud Terraform runner. It is a performance-testing subtree, not part of the main infra stack.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Env contract | `config.js` | `BASE_URL`, `AUTH_TOKEN`, shared headers, keyword list |
| Aggregate scenario mix | `load-test.js` | Multi-scenario VU plan, thresholds, writes `summary.json` |
| Focused entry scripts | `test-*.js` | Narrow runs for CRUD, search, recommendation |
| Scenario implementations | `scenarios/` | Request logic by use case |
| Remote runner infra | `terraform/main.tf` | GCP `k6-runner`, separate provider from `infra/` |

## CONVENTIONS
- `config.js` is the shared env surface; keep `BASE_URL` / `AUTH_TOKEN` usage centralized there.
- `load-test.js` mixes CRUD/search/recommendation traffic with scenario-specific thresholds; edits here affect the overall traffic model.
- Scenario files are intentionally lightweight and may swallow parsing failures to preserve latency/error sampling behavior.
- `terraform/` is for provisioning a GCP runner, not for the main application infrastructure.
- Results are expected in `summary.json` and console summary output; `results/` is for stored run artifacts.

## ANTI-PATTERNS
- Do not hardcode real tokens or production-only URLs into scripts.
- Do not treat `k6/terraform` as interchangeable with `infra/`; it uses a different cloud/provider and purpose.
- Do not tweak thresholds or VU mixes without considering the intended traffic distribution encoded in `load-test.js`.
- Do not bury request-shape changes in aggregate files when they belong in `scenarios/`.

## NOTES
- Blue/green deploy and app infra live elsewhere; `k6/` assumes a reachable API endpoint, not ownership of deployment.
- Some scenarios require auth headers, others are anonymous; keep that split visible in script names and headers.
