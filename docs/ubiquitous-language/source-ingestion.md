# Source / Ingestion

> 외부 기술 블로그 RSS를 수집하고, 신규 기술 게시글 후보를 내부 모델로 변환하는 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/domain/source`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 기술 블로그 | `TechBlog` | 수집 대상이 되는 회사/조직의 기술 블로그 소스. `companyName`, `blogUrl`, `rssUrl`, `logoUrl`, `lastCrawledAt`을 가진다. |
| RSS 피드 | `rssUrl`, `RssFeedReader` | 기술 블로그에서 게시글 목록을 가져오는 외부 피드 |
| 피드 아이템 | `RssFeedItem` | RSS 엔트리를 내부 기술 게시글 후보로 변환한 DTO. 제목, URL, 본문, 정제 본문, 발행일, 회사명, 썸네일 등을 가진다. |
| 크롤링 | `CrawlingService`, `rssCrawlingJob` | 모든 기술 블로그의 RSS를 수집해 신규 기술 게시글을 저장하는 작업 |
| 신규 기술 게시글 판별 | `findExistingUrls`, URL dedup | 이미 저장된 URL은 제외하고 신규 URL만 기술 게시글로 저장하는 정책 |
| 크롤링 잡 | `rssCrawlingJob` | `fetchAndSaveRssStep → extractSummaryStep → embedAndIndexStep` 순서의 전체 파이프라인 |
| 요약·임베딩 잡 | `summaryAndEmbeddingJob` | 이미 저장된 기술 게시글에 대해 요약/임베딩만 수행하는 별도 배치 |
| 크롤링 실패 알림 | `WebhookNotificationService` | 크롤링 실패 시 외부 채널로 운영 알림을 전송하는 개념 |

## 경계 메모

- `TechBlog`는 **RSS 소스/출처 루트**다.
- `Post.company`는 Source 컨텍스트의 진실 원천이 아니라, `TechBlog.companyName`의 **비정규화 스냅샷**이다.
- 장기적으로 `Source → Post`는 동기 직접 호출보다 `TechnicalPostDiscovered` 이벤트로 느슨하게 연결하는 것이 목표다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 마지막 크롤링 시각 | `lastCrawledAt` | 특정 기술 블로그가 마지막으로 성공 수집된 시각 |
| RSS 리더 | `RssFeedReader` | 외부 RSS를 읽어 `RssFeedItem` 목록으로 바꾸는 reader |
| 수집 step | `fetchAndSaveRssStep` | RSS 읽기와 신규 게시글 저장을 담당하는 배치 step |
| 실패 listener | `RssCrawlingJobListener` 계열 | 크롤링 실패 후 운영 알림/로그를 처리하는 후처리 지점 |

## 혼동 금지

- `기술 블로그`는 **콘텐츠 제공 출처**이고, `기술 게시글`은 **수집 결과 콘텐츠**다.
- `피드 아이템`은 Source 컨텍스트 내부 후보 모델이지, Post 컨텍스트의 aggregate가 아니다.
- `companyName`은 출처 표시명이지 독립 `Company` aggregate를 뜻하지 않는다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 블로그 글 | 기술 게시글 / 피드 아이템 | Source에서는 수집 전 후보와 수집 후 콘텐츠를 구분해야 한다 |
| 회사 | 기술 블로그 / 출처명 | 현재 모델의 루트는 `Company`가 아니라 `TechBlog`다 |
| 크롤링된 Post | 신규 기술 게시글 후보 / 저장된 기술 게시글 | Source와 Post 컨텍스트 경계를 흐리지 않기 위해 |

## 주요 근거 파일

- `src/main/java/com/techfork/domain/source/entity/TechBlog.java`
- `src/main/java/com/techfork/domain/source/dto/RssFeedItem.java`
- `src/main/java/com/techfork/domain/source/batch/RssFeedReader.java`
- `src/main/java/com/techfork/domain/source/config/RssCrawlingJobConfig.java`
- `src/main/java/com/techfork/domain/source/service/CrawlingService.java`
