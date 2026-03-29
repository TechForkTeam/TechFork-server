# EVALUATION TEST GUIDE

## OVERVIEW
`src/test/java/com/techfork/evaluation` is not ordinary integration testing; it is fixture-heavy search/recommendation quality evaluation with separate Gradle tasks, tags, and runtime assumptions.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Task split | `build.gradle` | `integrationTest`, `evaluationTest`, `evaluationSetup` |
| Integration base | `src/test/java/com/techfork/global/common/IntegrationTestBase.java` | `@Tag("integration")`, profile `integrationtest` |
| Recommendation evaluation base | `recommendation/RecommendationTestBase.java` | Loads fixtures, force-merges ES, warms caches 3x |
| Search evaluation base | `search/SearchEvaluationTestBase.java` | `@Tag("evaluation")`, profile `local-tunnel` |
| Fixtures / reports | `src/test/resources/fixtures/evaluation/`, `src/test/resources/evaluation-report-*.json` | Large inputs + checked-in outputs |

## CONVENTIONS
- `./gradlew test` excludes `integration`, `evaluation`, and `evaluation-setup` workflows by design.
- `IntegrationTestBase` is the normal controller/service integration path; evaluation suites are a different lane.
- Search evaluation uses `@ActiveProfiles("local-tunnel")`; do not silently swap it to `integrationtest`.
- Recommendation evaluation extends `IntegrationTestBase`, loads cached fixtures once, force-merges `posts` and `user_profiles`, then runs warmup before metrics.
- Search evaluation builds `SearchServiceImpl` directly per scenario and writes JSON reports into `src/test/resources/`.
- `evaluation-setup` jobs are prerequisite generators/exporters, not “extra assertions.” Treat them as data-prep workflows.

## COMMANDS
```bash
./gradlew integrationTest
./gradlew evaluationTest
./gradlew evaluationSetup
./gradlew test -PexcludeIntegration
```

## ANTI-PATTERNS
- Do not run evaluation suites as part of a casual unit/integration loop.
- Do not edit fixture JSON or evaluation reports by hand when the source data should be regenerated.
- Do not ignore the `local-tunnel` dependency for search evaluation; it is part of the test contract.
- Do not assume evaluation bases are cheap; they load large fixture sets and may warm Elasticsearch aggressively.

## NOTES
- Recommendation metrics center on Recall / nDCG / ILD across K values 4, 8, 30.
- Search metrics center on nDCG / Recall at 4, 8, 20 plus latency.
- If you only need normal application verification, stay in `domain/` or `global/` integration tests instead of this subtree.
