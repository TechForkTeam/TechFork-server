# SOURCE DOMAIN GUIDE

## OVERVIEW
`domain/source` owns RSS ingestion orchestration: crawl feeds, persist new posts, then hand off to summary and embedding steps.

## STRUCTURE
```text
source/
├── batch/       # feed reader, RSS->Post processor, bulk writer
├── config/      # job and step wiring
├── listener/    # job lifecycle hooks
├── scheduler/   # cron trigger
├── service/     # crawl execution + lock/job launch path
├── repository/  # TechBlog access
└── dto|entity/  # feed item and blog metadata
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Job topology | `config/RssCrawlingJobConfig.java` | `rssCrawlingJob` = fetch → summary → embed/index |
| Scheduler trigger | `scheduler/RssCrawlingScheduler.java` | Actual cron is `0 0 5 * * *`, zone `Asia/Seoul` |
| Full pipeline explainer | `docs/source-package.md` | Best human-readable walkthrough |
| Operational behavior | `docs/SCHEDULER_GUIDE.md` | Locking, notifications, troubleshooting; schedule text is partially stale |

## CONVENTIONS
- Step 1 `fetchAndSaveRssStep`: chunk 10, skip limit 10, `IllegalStateException` is `noSkip`.
- Step 2 `extractSummaryStep`: chunk 5, async processor/writer, summary executor fixed at 2 threads.
- Step 3 `embedAndIndexStep`: chunk 20, async processor/writer, embedding executor 10-20 threads.
- `summaryAndEmbeddingJob` intentionally skips the fetch step; keep it aligned with steps 2-3 only.
- MDC propagation is part of batch execution via `MdcTaskDecorator`; thread pool swaps are not cosmetic here.
- This package owns job orchestration, but summary/embedding readers/processors/writers are imported from `domain/post/batch`.

## ANTI-PATTERNS
- Do not move step-2/step-3 components into `source/` just because the job config imports them.
- Do not change cron, chunk sizes, skip limits, or thread counts without checking rate-limit and ops docs impact.
- Do not trust `docs/SCHEDULER_GUIDE.md` over code for schedule timing; the code path is the source of truth.
- Do not bypass the crawl service / lock / listener flow when adding manual job triggers.

## NOTES
- The scheduler comment and code both say daily 05:00 KST; some older docs still describe hourly crawling.
- If a change touches feed fetching, duplicate filtering, summary extraction, and ES indexing together, read `docs/source-package.md` first.
