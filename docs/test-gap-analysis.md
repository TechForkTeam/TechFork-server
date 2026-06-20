# TechFork 테스트 갭 분석

> 목적: DDD 전환과 테스트 개선 로드맵을 실행하기 전에, 현재 테스트가 어떤 영역을 보호하고 있고 어떤 영역이 비어 있는지 분석한다.  
> 관련 문서: [`docs/ubiquitous-language/README.md`](./ubiquitous-language/README.md), [`docs/ddd-test-refactoring-roadmap.md`](./ddd-test-refactoring-roadmap.md)  
> 기준 시점: **2026-06-03, #411 병합 후 `docs/#412` 시작 시점**

## 1. 분석 요약

현재 테스트는 **Controller/Repository/Service 단위의 기존 기능 회귀 테스트는 꽤 많이 쌓여 있지만, DDD 전환에 필요한 애그리거트 단위 테스트와 핵심 개인화/추천/검색 알고리즘 테스트가 부족한 상태**다.

테스트 보강 우선순위는 `docs/domain-strategy.md`의 하위 도메인 분류를 따른다. 즉, **핵심 하위 도메인인 개인화 기술 콘텐츠 발견(Search/Recommendation), 사용자 관심/행동 기반 개인화 모델링(Personalization Profile/Activity), 기술 게시글 이해/풍부화(Post enrichment)** 를 먼저 보호한다.

요약하면 다음과 같다.

| 영역 | 현재 상태 | DDD 전환 관점 평가 |
|---|---|---|
| Activity | 서비스, repository, controller 테스트가 충분하고 `Bookmark`/`query` 용어 정리도 대부분 반영됨 | 4.1을 `Bookmark → ReadPost → SearchHistory` slice로 나눠 진행하기 좋음 |
| Source / Ingestion | RSS reader, processor, writer, scheduler, crawling service 테스트가 잘 있음 | 파이프라인 보호 수준 좋음 |
| Post / Content | 조회 API/repository, aggregate, summary/embedding pipeline 테스트가 tracked 상태로 보강됨 | 후속은 ContentChunker, rollback/운영 edge case 중심 |
| User Account | 온보딩/관심사/계정 프로필/탈퇴와 이벤트 발행이 강함 | 사용자 계정 애그리거트와 후처리 seam은 비교적 잘 보호되어 있다 |
| Personalization Profile | 활동 수집/분석/생성/이벤트 발행/AFTER_COMMIT 경계 테스트가 보강됨 | 다음 리스크는 parsing edge case와 재시도/outbox 같은 운영 복구 정책 |
| Recommendation | 조회, 이벤트 리스너, LlmRecommendationService 핵심 흐름 테스트가 생김 | MMR 단위 테스트, command/scheduler/history/repository 정책 보강 필요 |
| Search | `SearchServiceImplTest`로 일반/개인화 검색 핵심 회귀가 일부 보호됨 | controller/API contract와 추가 ranking edge case 보강 필요 |
| Auth / Security | 토큰/필터/컨트롤러 테스트는 비교적 강함 | OAuth handler/OIDC 일부 보강 여지 |
| Notification | 엔티티만 있고 테스트/기능이 약함 | 현재는 낮은 우선순위 |
| Admin / Ops | 개발자 토큰 중심 테스트 있음 | 배치 수동 실행 API 커버 보강 필요 |

가장 중요한 갭은 다음 5개다.

1. **MmrService 단위 테스트 부재**
2. **Recommendation command/scheduler/history/repository 정책 테스트 부족**
3. **Search controller/API contract와 ranking edge-case 테스트 부족**
4. **Personalization Profile parsing edge-case와 운영 복구 정책 테스트 부족**
5. **TechnicalPostIndexed 같은 후속 이벤트 contract 테스트 부재**

---

## 2. 테스트 실행 구조

`build.gradle` 기준 테스트 태스크는 다음과 같다.

| 태스크 | 태그 설정 | 용도 |
|---|---|---|
| `test` | 기본적으로 `evaluation`, `evaluation-setup` 제외. `-PexcludeIntegration`가 있을 때 `integration`도 제외 | 일반 테스트 실행 |
| `integrationTest` | `integration` 포함, `evaluation`, `evaluation-setup` 제외 | 통합 테스트 실행 |
| `evaluationTest` | `evaluation` 포함, `integration`, `evaluation-setup` 제외 | 검색/추천 품질 평가 |
| `evaluationSetup` | `evaluation-setup` 포함 | 평가 fixture 생성/export |

권장 실행 명령:

```bash
./gradlew test -PexcludeIntegration
./gradlew integrationTest
./gradlew evaluationTest
./gradlew evaluationSetup
```

주의:

- `src/test/java/com/techfork/evaluation/AGENTS.md`에 따르면 evaluation 테스트는 일반 통합 테스트가 아니다.
- evaluation suite는 fixture, Elasticsearch, local-tunnel profile, 리포트 파일을 다루므로 일반 리팩터링 검증 루프에서 제외한다.
- `build.gradle`상 `test` 태스크는 `-PexcludeIntegration`가 있을 때만 `integration` 태그를 제외한다. 빠른 단위 테스트 루프는 `./gradlew test -PexcludeIntegration`를 명시하는 것이 안전하다.

---

## 3. 현재 테스트 인벤토리

### 3.1 tracked 테스트 개요

`git ls-files src/test/java` 기준 tracked 테스트 인벤토리다.

| 영역 | 파일 수 | `@Test`/`@ParameterizedTest` 수 | 비고 |
|---|---:|---:|---|
| activity | 20 | 69 | Bookmark/ReadPost/SearchHistory slice와 첫 읽기 정책까지 커버 |
| domain/admin | 0 | 0 | 개발자 토큰 테스트는 Auth / Security로 이동 |
| auth | 13 | 85 | AuthCommandService, KakaoLoginCommandService, DeveloperTokenController, KakaoOAuthService, Auth/Security integration |
| post | 20 | 118 | aggregate, query, summary/embedding pipeline까지 tracked 테스트로 보강됨 |
| domain/recommendation | 5 | 17 | 조회/컨버터 + 이벤트 리스너 + LlmRecommendationService 핵심 흐름 |
| domain/search | 1 | 2 | SearchServiceImpl 핵심 회귀 일부 |
| domain/source | 10 | 38 | RSS/배치/스케줄러/락/웹훅 커버 좋음 |
| useraccount | 14 | 93 | User Account service/controller/repository/event seam 커버 |
| personalization | 8 | 18 | 활동 수집/분석/생성/이벤트/스케줄러 경계 커버 |
| global | 5 | 6 | shared test base/configuration/util; Auth/Security 테스트는 `auth`로 이동 |
| evaluation | 27 | 16 | 검색/추천 품질 평가 및 fixture setup |

### 3.2 tracked 테스트 주의사항

Post summary/embedding pipeline 테스트는 현재 tracked 상태로 포함되어 있다.

대표 파일:

```text
src/test/java/com/techfork/post/application/batch/PostSummaryProcessorTest.java
src/test/java/com/techfork/post/infrastructure/batch/PostSummaryReaderTest.java
src/test/java/com/techfork/post/infrastructure/batch/PostSummaryWriterTest.java
src/test/java/com/techfork/post/application/batch/PostEmbeddingProcessorTest.java
src/test/java/com/techfork/post/infrastructure/batch/PostEmbeddingWriterTest.java
```

의미:

- Post summary/embedding pipeline은 DDD 리팩터링 기본 안전망으로 볼 수 있다.
- 남은 갭은 `ContentChunkerService`, rollback/운영 edge case, 후속 이벤트 contract 중심으로 이동했다.

---

## 4. 컨텍스트별 상세 분석

### 4.1 Activity 컨텍스트

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `ReadHistoryCommandServiceTest` | unit/mock | 검색 기록 저장, 예외 |
| `BookmarkCommandServiceTest` | unit/mock | 북마크 추가/중복 방지/삭제, 예외 |
| `BookmarkQueryServiceTest` | unit/mock | 북마크 목록, 커서 페이징, 키워드 조합 |
| `BookmarkTest` | unit | 북마크 생성 시 상태 보존 |
| `BookmarkIntegrationTest` | integration | 북마크 API 저장/조회/삭제 흐름 |
| `ReadPostCommandServiceTest` | unit/mock | 읽기 저장, 첫 읽기 조회수 증가, 예외 |
| `ReadPostQueryServiceTest` | unit/mock | 읽은 게시글 목록, 키워드/북마크 여부 조합 |
| `ReadPostFirstReadPolicyTest` | unit/mock | 첫 읽기 판별 규칙 |
| `ReadPostTest` | unit | 읽기 기록 생성 시 상태 보존 |
| `ReadPostRepositoryTest` | JPA | 최근 읽은 글, 중복 읽기 저장, 커서 조회/중복 제거 |
| `ReadPostIntegrationTest` | integration | ReadPost API 저장/조회 흐름 |
| `BookmarkRepositoryTest` | JPA | 북마크 커서 조회, 북마크된 postId 조회, user+post 유일성 |
| `SearchHistoryRepositoryTest` | JPA | 최근 검색 기록 조회 |
| `SearchHistoryRequestTest` | unit | `query` canonical field + legacy `searchWord` alias 역직렬화 |
| `SearchHistoryIntegrationTest` | integration | SearchHistory API 흐름 |

#### 평가

Activity는 현재 가장 보호가 잘 된 영역 중 하나다.  
`Bookmark` 용어, `ReadPost` slice 분리, `query` canonical field는 이미 코드/테스트에 반영되어 있고, 4.1의 남은 작업은 사실상 `SearchHistory` slice 쪽으로 좁혀졌다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P1 | `BookmarkTest` | 같은 사용자 + 같은 게시글 조합 유일성 같은 aggregate 관점 의도를 더 직접적으로 표현 가능 |
| P1 | `SearchHistory` 도메인 엔티티 단위 테스트 | 애그리거트/record aggregate 관점의 테스트 명확화 |
| P1 | SearchHistory canonical `query` / legacy `searchWord` 호환 범위 문서화 | API 역호환 정책을 언제까지 유지할지 명확화 필요 |
| P2 | 북마크 migration 적용 검증 메모 | `bookmarks` / `bookmarked_at` rename이 운영 환경에 모두 반영됐는지 확인 필요 |

#### 추천 다음 작업

```text
1. Activity 기존 테스트 전체 통과 확인
2. Bookmark aggregate/entity 테스트 보강 여부 결정
3. SearchHistory alias 정책과 기록 slice를 별도 작업으로 정리
4. Bookmark → ReadPost 완료 후 SearchHistory 순서로 4.1 후속 작업 진행
5. 기존 Activity 테스트가 전부 통과하는지 확인
```

---

### 4.2 Source / Ingestion 컨텍스트

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `RssFeedReaderTest` | unit/mock | RSS 수집, 실패 격리, timeout, 중복 URL 제거, 본문/썸네일 추출 |
| `RssToPostProcessorTest` | unit/mock | `RssFeedItem → Post` 변환 |
| `PostBatchWriterTest` | unit/mock | bulk insert SQL/binding |
| `RssCrawlingJobIntegrationTest` | integration-ish | job/step wiring, 실행 순서 |
| `TechBlogTest` | domain unit | logo URL fallback |
| `RssCrawlingJobListenerTest` | unit/mock | MDC, cache warmup, failure webhook |
| `RssCrawlingSchedulerTest` | unit/reflection | scheduler 호출, cron metadata |
| `CrawlingServiceTest` | unit/mock | lock 획득/실패, job launch, unlock 보장 |
| `WebhookNotificationServiceTest` | unit/mock | webhook disabled/enabled/failure |

#### 평가

Source/Ingestion은 매우 잘 보호되어 있다.  
`TechBlog` 애그리거트와 RSS ACL 역할도 테스트로 어느 정도 보호된다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P1 | `TechBlog` URL uniqueness는 repository/integration 관점에서 명시 테스트 부족 | DB unique constraint 회귀 보호 |
| P1 | `TechnicalPostDiscovered/Saved` 이벤트 도입 시 contract test 필요 | Source → Post 이벤트 분리 대비 |
| P2 | 실제 Rome RSS parser fixture 기반 테스트 보강 | 현재 unit mock 중심이면 실제 feed variation 회귀에 취약할 수 있음 |

#### 추천 다음 작업

Source는 DDD 전환 초반에는 큰 변경을 피하고, 나중에 이벤트 분리 시 contract test를 추가한다.

---

### 4.3 Post / Content 컨텍스트

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `PostControllerIntegrationTest` | integration | v1 API, 상세/회사/최근/회사별 조회, auth 포함 응답 |
| `PostControllerV2IntegrationTest` | integration | v2 회사 목록, 다중 회사, 커서 페이징, 최신/인기 정렬 |
| `PostRepositoryTest` | JPA | company/recent/popular/detail 쿼리, cursor, company detail |
| `PostQueryServiceTest` | unit/mock | 목록/상세 조회, 키워드/북마크 여부 조합 |
| `PostTest` | unit | RssFeedItem 생성, summary 갱신, keyword 재구성 |
| `PostSummaryProcessorTest` | unit/mock | summary/keyword 반영, 예외 전파 시 기존 상태 유지 |
| `PostSummaryReaderTest` | unit/mock | load-once reader, 순차 반환, 빈 결과 처리 |
| `PostSummaryReaderDataJpaTest` | JPA | `summary IS NULL OR ''` query contract, keyword fetch join |
| `PostSummaryWriterTest` | unit/mock | summary update / keyword delete / insert JDBC binding |
| `PostSummaryWriterDataJpaTest` | JPA | H2에서 summary 저장과 keyword 재구성 검증 |
| `SummaryExtractionServiceTest` | unit/mock | LLM JSON parsing, invalid JSON fail-fast, 긴 본문 제한 |

#### 평가

Post 조회 API와 repository는 강하다.  
이제 `Post` aggregate와 summary pipeline 핵심 경계(`Processor` / `Reader` / `Writer` / `SummaryExtractionService`)는 기본 안전망이 생겼다.  
다만 검색/추천의 핵심 입력인 **embedding pipeline**은 여전히 거의 보호되지 않는다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P0 | `PostEmbeddingProcessorTest` | 제목/요약/청크 임베딩으로 `PostDocument` 생성하는 핵심 pipeline 보호 필요 |
| P0 | `PostEmbeddingWriterTest` | Elasticsearch bulk index + `embeddedAt` update 회귀 위험 큼 |
| P1 | `ContentChunkerServiceTest` | semantic search 품질에 직접 영향 |
| P1 | `PostDocumentTest` | publishedAt serialization, embedding/chunk projection 보호 |
| P1 | `PostSummaryWriter` rollback integration test | update/delete/insert 3단계 JDBC write의 원자성 확인 필요 |
| P2 | `PostKeywordRepositoryTest` | 키워드 bulk 조회가 여러 서비스 응답 조합에 쓰임 |

#### 현재 우려사항 (merge blocker 아님)

- summary pipeline 상태가 아직 `summary IS NULL OR ''`, `embeddedAt IS NULL` 조합으로 암묵적으로 표현된다.
- malformed LLM JSON은 이제 fail-fast로 막히지만, 예외 타입은 여전히 `LlmException`을 재사용해 transport 실패와 response-format 실패를 같은 버킷으로 본다.
- `PostSummaryReader`는 여전히 backlog를 한 번에 `List<Post>`로 올리는 구조라, 미요약 게시글이 크게 늘면 paging/streaming 전환 검토가 필요하다.

#### 추천 추가 테스트

```text
PostTest
- RssFeedItem으로 기술 게시글을 생성한다.
- 생성 시 company는 TechBlog.companyName 또는 RssFeedItem.company 스냅샷을 가진다.
- 요약과 짧은 요약을 갱신한다.
- 게시글 키워드를 새 목록으로 재구성한다.
- 빈 키워드 목록이면 기존 키워드를 모두 제거한다.
- 조회수를 증가시킨다.
```

```text
PostEmbeddingProcessorTest
- 제목과 요약을 각각 임베딩한다.
- plainContent를 chunk로 분할한다.
- 유효한 chunk만 batch embedding한다.
- PostDocument에 titleEmbedding, summaryEmbedding, contentChunks를 채운다.
- 임베딩 실패 시 예외를 전파한다.
```

```text
ContentChunkerServiceTest
- 짧은 본문은 하나의 chunk로 유지한다.
- 긴 본문은 MAX_CHUNK_SIZE 기준으로 분할한다.
- overlap을 유지한다.
- HTML/Markdown 구조 기준 분할을 우선한다.
- 빈/null 본문 처리 정책을 검증한다.
```

---

### 4.4 User Account / Personalization Profile 경계

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `OnboardingControllerIntegrationTest` | integration | 관심사 목록, 온보딩 완료, validation |
| `UserControllerIntegrationTest` | integration | 계정 프로필 조회/수정, 탈퇴 |
| `UserRepositoryTest` | JPA | 관심사 fetch, 활성 사용자 조회, 탈퇴 사용자 제외 |
| `UserInterestCategoryRepositoryTest` | JPA | 관심 카테고리/키워드 fetch, cascade, mapping |
| `InterestCommandServiceTest` | unit/mock | 관심사 저장, 기존 관심사 clear, invalid keyword category, 관심사 수정 이벤트 발행 |
| `UserCommandServiceTest` | unit/mock | 온보딩, 계정 프로필 수정, 탈퇴, 온보딩/탈퇴 이벤트 발행 |
| `PersonalizationProfileEventListenerTest` | unit/mock | 온보딩/관심사 변경 이벤트의 `AFTER_COMMIT` 개인화 프로필 생성 요청 |
| `UserAuthCacheInvalidationListenerTest` | unit/mock | 온보딩 `AFTER_COMMIT` 캐시 evict, 탈퇴 `BEFORE_COMMIT + AFTER_COMMIT` 캐시 evict |
| `UserAccountAfterCommitEventIntegrationTest` | integration | 커밋 후 후처리 실행, 롤백 시 후처리 미실행, 탈퇴 evict 실패 시 트랜잭션 롤백 |
| `UserQueryServiceTest` | unit/mock | 계정 프로필 조회 |
| `PersonalizationProfileServiceTest` | unit/mock | 프로필 생성 성공 시 이벤트 발행, 생성 실패 시 이벤트 미발행 |
| `UserActivityCollectorTest` | unit/mock | 관심사/읽은 글/북마크/검색 기록 활동 데이터 수집 |
| `PersonalizationProfileAnalyzerTest` | unit/mock | LLM 응답 profileText/keyKeywords 분석 |
| `PersonalizedProfileGeneratorTest` | unit/mock | 활동 수집, 분석, 임베딩, projection 저장 orchestration |
| `PersonalizedProfileGeneratedEventTest` | unit | 이벤트 payload defensive copy / null normalization |
| `PersonalizedProfileGeneratedAfterCommitIntegrationTest` | lightweight integration | 커밋 후 추천 리스너 실행, 롤백 시 미실행 |
| `PersonalizationProfileSchedulerTest` | unit/mock | 활성 사용자 대상 주기 재생성 요청 |
| `evaluation/search/setup/PersonalizationProfileServiceTest` | evaluation-setup | 테스트 사용자 개인화 프로필 생성용 setup |

#### 평가

- **User Account 쪽**은 온보딩, 관심사, 계정 프로필, 탈퇴 흐름과 이벤트 발행이 비교적 잘 보호되어 있다.
- **Personalization Profile 쪽**은 활동 수집/분석/생성 orchestration과 이벤트 발행/커밋 후 경계 테스트가 추가되었다.
- **Personalization → Recommendation seam**은 `PersonalizedProfileGeneratedEvent` publisher/listener/lightweight integration test로 보호한다.
- **Auth cache seam**은 온보딩/탈퇴 이벤트 리스너 테스트와 통합 테스트로 보호한다. 탈퇴 캐시 evict 실패는 트랜잭션 롤백으로 검증한다.
- evaluation setup용 `PersonalizationProfileServiceTest`는 여전히 별도 목적이므로, 일반 unit test lane과 구분해서 본다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| 완료 | `PersonalizedProfileGeneratedEvent` publisher/listener/integration test | 개인화 프로필 생성과 추천 생성 이벤트 경계 보호 |
| P1 | LLM 응답 parsing edge-case 테스트 | `### PROFILE`, `### KEYWORDS` 변형/누락 시 품질/장애 영향 |
| P1 | `UserInterestCategory/UserInterestKeyword` 도메인 테스트 | 관심 키워드가 카테고리에 속해야 한다는 규칙 명시 |
| 완료 | `PersonalizationProfileSchedulerTest` | 매일 06:00 KST active user personalization profile regeneration 보호 |
| P2 | `InterestQueryServiceTest` | 관심사 조회 변환 로직 보호 |

#### 추천 추가 테스트

```text
PersonalizationProfileServiceTest / PersonalizedProfileGenerated* tests
- 개인화 프로필 생성 성공 후 `PersonalizedProfileGeneratedEvent`를 발행한다.
- 개인화 프로필 생성 실패 시 이벤트를 발행하지 않는다.
- 이벤트 payload는 profileVector/keyKeywords 스냅샷을 보존한다.
- 이벤트 리스너는 커밋 후 Recommendation 생성을 트리거하고, 롤백 시 실행되지 않는다.
- 추천 생성 실패는 Recommendation 리스너 안에서 격리되어 프로필 저장 결과를 깨뜨리지 않는다.
```

```text
UserTest
- 소셜 사용자 생성 시 기본 role은 USER다.
- 소셜 사용자 생성 시 기본 status는 PENDING이다.
- 온보딩 완료 시 ACTIVE가 된다.
- 탈퇴 시 개인정보가 null 처리되고 WITHDRAWN이 된다.
- 재활성화 시 PENDING이 된다.
```

---

### 4.5 Recommendation 컨텍스트

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `RecommendationControllerIntegrationTest` | integration | 추천 목록 조회, 랭킹 순서, 북마크 상태 조합 |
| `RecommendationQueryServiceTest` | unit/mock | 추천 목록 조회, 북마크 상태 조합, 빈 목록 |
| `RecommendationConverterTest` | unit/mock | thumbnail optimization |
| `PersonalizedProfileGeneratedEventListenerTest` | unit/mock | 이벤트 수신 후 스냅샷 기반 추천 생성, 예외 격리, AFTER_COMMIT annotation |
| `LlmRecommendationServiceTest` | unit/mock | 프로필 없음/벡터 없음, 읽은 글 제외, RRF/MMR 흐름, 기존 추천 이력화, 새 추천 저장 |
| evaluation recommendation tests | evaluation | K, lambda, MMR candidate size, title/summary ratio 품질 비교 |

#### 평가

조회 쪽과 `LlmRecommendationService` 핵심 흐름은 일부 보호되었다.
다만 `MmrService` 단위 테스트, 추천 command/scheduler/history/repository 정책 테스트는 아직 부족하다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P0 | `MmrServiceTest` | 추천 다양성/관련성 알고리즘의 핵심 |
| P1 | `LlmRecommendationServiceTest` edge-case 확장 | 기본 흐름은 보호되었고, 실패/빈 후보/time-decay 세부 정책은 추가 보강 여지 |
| P1 | `RecommendationCommandServiceTest` | 수동 재생성 API 흐름 보호 |
| P1 | `RecommendationSchedulerTest` | 매일 07:00 KST active user 추천 생성 보호 |
| P1 | `RecommendedPostRepositoryTest` | rank order, deleteByUser, unique user-post 보호 |
| P1 | `RecommendationHistoryTest` | fromRecommendedPost, clicked marking 보호 |
| P2 | `RecommendationSet` 개념 도입 시 aggregate test | DDD 모델 전환 시 필요 |

#### 추천 추가 테스트

```text
MmrServiceTest
- 후보가 null/empty이면 빈 결과를 반환한다.
- 최종 추천 수는 mmrFinalSize와 후보 수 중 작은 값이다.
- 첫 번째 선택은 topK 정책을 따른다.
- lambda가 높으면 관련성 중심으로 선택한다.
- 이미 선택된 문서와 유사한 후보는 diversity penalty를 받는다.
```

```text
LlmRecommendationServiceTest
- 사용자 개인화 프로필이 없으면 0을 반환한다.
- profileVector가 없으면 0을 반환한다.
- 이벤트 스냅샷 경로는 ES refresh 직후 프로필 재조회에 의존하지 않는다.
- 읽은 게시글은 후보 검색 filter에 포함된다.
- vectorHits와 keywordHits를 RRF로 합친다.
- 기존 추천은 RecommendationHistory로 저장된다.
- 기존 추천은 삭제되고 새 추천이 저장된다.
- Elasticsearch 검색 실패/빈 후보/time decay 세부 정책은 추가 보강 여지다.
```

---

### 4.6 Search 컨텍스트

#### 현재 테스트

`src/test/java/com/techfork/domain/search/service/SearchServiceImplTest.java`가 추가되어 일반/개인화 검색 핵심 회귀 일부를 보호한다.

evaluation suite도 별도로 유지한다.

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `SearchPhase1FieldWeightTest` | evaluation | field weight 품질 비교 |
| `SearchPhase2FieldWeightDetailTest` | evaluation | field weight 세부 조정 |
| `SearchPhase3ChunkStructureTest` | evaluation | chunk 구조 품질 비교 |
| `SearchPhase4KnnParameterTest` | evaluation | KNN parameter 품질/latency 비교 |
| `SearchPhase5QueryStructureTest` | evaluation | bool vs dis_max 구조 비교 |

#### 평가

Search는 이제 `SearchServiceImplTest`로 빠른 일반 회귀 일부를 확보했다.
evaluation suite는 무겁고 runtime/profile/fixture 의존이 강하므로 품질 튜닝용으로 분리 유지한다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| 완료 | `SearchServiceImplTest` 기본 단위 테스트 | 일반 검색/개인화 검색 핵심 흐름 일부 보호 |
| P1 | RRF/ranking edge-case 테스트 | 검색 결과 순위 품질과 직접 연결 |
| P1 | 개인화 fallback/reranking 확장 테스트 | Personalization Profile projection 소비 경계 보강 |
| P1 | BM25 query builder 구조 테스트 | dis_max/exact/fuzzy/chunk 구조 회귀 방지 |
| P1 | Semantic KNN field/boost 테스트 | title/summary/chunk embedding field 회귀 방지 |
| P1 | metadata attachment 테스트 | viewCount, isBookmarked 조합 보호 |
| P2 | `SearchControllerIntegrationTest` | API contract 보호 |

#### 추천 추가 테스트

```text
SearchServiceImplTest
- searchGeneral은 BM25와 semantic 결과를 RRF로 결합한다.
- searchPersonalized는 프로필이 없으면 fallback한다.
- searchPersonalized는 프로필이 있으면 personalScore로 rerank한다.
- stripVectors는 응답에서 titleVector/summaryVector를 제거한다.
- attachPostMetadata는 viewCount와 isBookmarked를 채운다.
```

```text
SearchControllerIntegrationTest
- 비로그인 검색은 일반 검색을 호출한다.
- 로그인 검색은 개인화 검색을 호출한다.
- bm25 debug endpoint가 동작한다.
- semantic debug endpoint가 동작한다.
```

---

### 4.7 Auth / Security 컨텍스트

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `AuthCommandServiceTest` | unit/mock | refresh, logout, developer token command use cases |
| `KakaoLoginCommandServiceTest` | unit/mock | iOS 직접 Kakao login 신규/기존 사용자 |
| `KakaoOAuthServiceTest` | unit/mock | Kakao user info response mapping success/failure |
| `KakaoSocialIdTest` | unit | Kakao REST/OIDC social id 정규화 |
| `AuthControllerIntegrationTest` | integration | refresh/logout/kakao login API |
| `DeveloperTokenControllerIntegrationTest` | integration | developer token 생성, 권한/인증 실패 |
| `SecurityIntegrationTest` | integration | 인증/인가, 토큰 오류, 권한, 탈퇴 사용자 |
| `JwtUtilTest` | unit | refresh token 단독 발급과 token type |
| `JwtAuthenticationFilterTest` | unit/mock | access token filter, cache hit/miss, invalid token |
| `RefreshTokenCookieWriterTest` | unit | refresh token cookie 작성/삭제 wire contract |
| `OAuth2AuthenticationSuccessHandlerTest` | unit/mock | OAuth2 로그인 성공 refresh token 발급/저장과 redirect |
| `OAuth2AuthenticationFailureHandlerTest` | unit/mock | OIDC 로그인 실패 redirect |
| `OAuth2LoginRedirectUrlFactoryTest` | unit | success/failure redirect URL 조립, token query 제거, email encoding |
| `OAuth2LoginRefreshTokenIssuerTest` | unit/mock | OAuth2 로그인 성공 경로의 refresh token 단독 발급 |
| `OAuth2LoginRefreshTokenWriterTest` | unit/mock | OAuth2 로그인 refresh token 저장과 cookie writer 위임 |
| `CustomOidcUserServiceTest` | unit/mock | Kakao/Apple OIDC 사용자 생성/재사용/재활성화 |
| `OidcSocialIdentityExtractorTest` | unit | Kakao/Apple OIDC claim에서 소셜 식별자 추출 |
| `HttpCookieOAuth2AuthorizationRequestRepositoryTest` | unit/mock | OAuth authorization request cookie 저장/로드/삭제 |
| `UserAuthCacheStoreTest` | unit/mock | auth cache serialization/deserialization |
| `UserAuthCacheInvalidationListenerTest` | unit/mock | User Account 이벤트 기반 auth cache eviction |

#### 평가

Auth / Security는 1차 DDD 경계 정리 이후 비교적 안정적이다.
`auth` 최상위 컨텍스트와 `auth/security` shared kernel은 테스트로 대부분 보호되어 있고,
OAuth/OIDC handler, redirect URL factory, refresh token issuer/writer, cookie writer가 단위 테스트 가능한 seam으로 분리되었다.

OAuth 성공 redirect는 access token을 URL에 싣지 않고 refresh token cookie를 발급한 뒤,
callback 이후 `/api/v1/auth/refresh`로 access token을 재발급받는 계약으로 정리되었다.
Cookie 속성은 `RefreshTokenCookieWriterTest`로 단위 보호가 생겼지만,
실제 브라우저 cross-origin 환경에서의 통합 검증은 별도 경량 경로가 필요하다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P1 | Auth / Security 통합 테스트의 경량 MySQL+Redis 기반 검증 경로 | 현재 auth controller/security integration도 Elasticsearch Testcontainers까지 기동할 수 있어 auth-only 회귀 검증 비용과 실패면이 크다 |
| P1 | withdraw/reactivate 이후 refresh token/auth cache 통합 정책 테스트 | User 상태 전환과 보안 정책 연결 |
| P2 | OAuth success redirect + refresh 연동 통합 계약 | success redirect에는 `registered`, `email`만 남고 access token은 refresh API로만 받는 end-to-end 계약 보호 |
| P2 | 운영 env/secret 변경 계약 문서화 | `JWT_LOGIN_SUCCESS_REDIRECT_URI(_DEV)` 누락 시 OAuth callback 장애가 배포 후 발견될 수 있다 |
| P3 | Cookie 속성(SameSite/Secure/Domain/Max-Age) 통합 검증 | 단위 테스트는 있으나 실제 브라우저/cross-origin 쿠키 전달 계약은 e2e 또는 경량 통합 경로가 필요 |

---

### 4.8 Admin / Ops 컨텍스트

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| Source tests | unit/integration | crawling service, job, scheduler, webhook |

#### 평가

개발자 토큰 API는 Auth / Security 소유 테스트로 이동했다.
AdminController의 수동 배치 실행 API는 상대적으로 약하다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P1 | `runSummaryAndEmbeddingBatch` API success/failure | 운영 수동 배치 실행 보호 |
| P1 | `crawlRss` API success/failure | 운영 수동 크롤링 보호 |
| P2 | 관리자 권한 boundary 추가 케이스 | 보안 회귀 방지 |

---

### 4.9 Notification 컨텍스트

#### 현재 테스트

Notification domain에는 현재 실질 테스트가 없다.

```text
src/main/java/com/techfork/domain/notification/entity/NotificationToken.java
```

#### 평가

현재 기능이 약하므로 우선순위는 낮다.  
추천 알림 기능이 본격화되면 테스트가 필요하다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P3 | `NotificationTokenTest` | token 활성/비활성 정책 생길 때 필요 |
| P3 | Notification repository/service test | 알림 기능 도입 시 필요 |

---

## 5. DDD 애그리거트 기준 테스트 갭

현재 테스트는 서비스/컨트롤러 중심이 많고, 애그리거트 루트 단위 테스트가 부족하다.

| 애그리거트 / 모델 | 현재 테스트 | 갭 |
|---|---|---|
| `TechBlog` | `TechBlogTest` 있음 | URL uniqueness repository 테스트 보강 정도 |
| `Post` | `PostTest`, `PostViewCountCommandServiceTest`, query/batch tests | 기술 게시글 생성/요약/키워드/조회수 기본 보호 완료. 후속은 chunking/운영 edge case 중심 |
| `PostKeyword` | `PostTest`, `PostKeywordLookupServiceTest` | Post 내부 엔티티와 조회 조합 경로로 기본 보호 |
| `User` | `UserTest`, `UserCommandServiceTest` 중심 | 상태 전이 기본 보호 완료. 추가 VO/ID reference 전환 시 보강 |
| `UserInterestCategory/Keyword` | repository/service 중심 | User Account 도메인 규칙 테스트 보강 필요 |
| `PersonalizationProfileDocument` | `PersonalizationProfileServiceTest` + evaluation setup | projection 자체 직접 테스트/세부 parsing 검증은 더 보강 가능 |
| `ReadPost` | `ReadPostTest`, `ReadPostFirstReadPolicyTest`, `ReadPostCommandServiceTest`, `ReadPostQueryServiceTest`, `ReadPostRepositoryTest`, `ReadPostIntegrationTest` | 패키지 slice와 첫 읽기 정책까지 분리되었고, 이후 관심사는 동시성/ID reference 같은 별도 이슈다 |
| `Bookmark` | `BookmarkTest`, `BookmarkRepositoryTest`, `BookmarkCommandServiceTest`, `BookmarkQueryServiceTest`, `BookmarkIntegrationTest` 중심 | 패키지 slice 분리 이후에도 ID reference 전환 같은 aggregate 경계 재설계는 별도 이슈로 다루는 편이 안전 |
| `SearchHistory` | `SearchHistoryRequestTest`, `SearchHistoryRepositoryTest`, `ReadHistoryCommandServiceTest`, `SearchHistoryIntegrationTest` | record aggregate 단위 테스트는 선택 |
| `RecommendedPost` | query/controller 중심 | 생성/순위/unique 정책 보강 필요 |
| `RecommendationHistory` | 없음 | 이력화/fromRecommendedPost/click 테스트 필요 |
| `RecommendationSet` 개념 | 없음 | DDD 모델 도입 시 신규 테스트 필요 |

---

## 6. 우선순위별 테스트 백로그

### P0. DDD 전환 전 반드시 보강

| 테스트 | 목적 | 선행/연결 작업 |
|---|---|---|
| `MmrServiceTest` | 추천 알고리즘 핵심 보호 | Recommendation DDD 전환 |
| `RecommendationCommandServiceTest` | 수동 추천 재생성 use case 보호 | Recommendation application 경계 정리 |
| `RecommendationSchedulerTest` | 일일 추천 생성 스케줄 보호 | 운영 배치 안정화 |
| `RecommendedPostRepositoryTest` | rank order/delete/unique 보호 | RecommendationSet 도입 전 |
| `RecommendationHistoryTest` | 이력화/click 기록 보호 | 추천 이력 모델 정리 |
| `SearchControllerIntegrationTest` | 검색 API contract 보호 | Search API 경계 정리 |
| `TechnicalPostIndexed` event contract tests | 색인 완료 후속 이벤트 보호 | Phase 6 후속 이벤트 도입 |

### P1. DDD 전환 중 보강

| 테스트 | 목적 |
|---|---|
| `ContentChunkerServiceTest` | semantic search 품질 입력 보호 |
| `PostSummaryWriter` rollback integration test | summary/keyword JDBC write 원자성 보호 |
| `Search/RRF ranking edge-case tests` | 검색 순위 정책 확장 보호 |
| `PostKeywordRepositoryTest` | 키워드 조회 조합 보호 |

### P2. 안정화/운영 보강

| 테스트 | 목적 |
|---|---|
| `AdminController` batch endpoint tests | 수동 배치/크롤링 운영 API 보호 |
| Apple/OIDC handler tests | OAuth 표면 보호 |
| `HttpCookieOAuth2AuthorizationRequestRepositoryTest` | OAuth cookie edge case 보호 |
| RSS real fixture parser test | 실제 feed variation 보호 |
| 이벤트 contract tests | 이벤트 기반 분리 후 publisher/subscriber contract 보호 |

### P3. 기능 확장 시 보강

| 테스트 | 목적 |
|---|---|
| `NotificationTokenTest` | 추천 알림/푸시 기능 도입 시 필요 |
| Notification service/repository tests | 알림 기능 확장 시 필요 |
| `RecommendationSetTest` | 추천 목록 애그리거트 도입 시 필요 |

---

## 7. 테스트 작성 순서 제안

`docs/ddd-test-refactoring-roadmap.md`의 실행 순서를 테스트 관점으로 더 구체화하면 다음과 같다.

```text
1. 현재 문서/테스트 인벤토리와 DDD 경계 최신화 유지
2. MmrServiceTest 작성
3. RecommendationCommand/Scheduler/History/Repository 테스트 보강
4. SearchControllerIntegrationTest 및 ranking edge-case 테스트 보강
5. PersonalizationProfile parsing edge-case와 outbox/retry 필요성 검토
6. TechnicalPostIndexed 이벤트 contract 테스트 준비
```

가장 먼저 시작할 실제 작업 단위는 다음이 좋다.

```text
작업 1: Activity/Bookmark 리팩터링 안전망
- 기존 BookmarkCommandServiceTest / BookmarkQueryServiceTest 확인
- ReadPostCommandServiceTest / ReadPostQueryServiceTest / ReadPostIntegrationTest 확인
- 기존 SearchHistoryIntegrationTest 확인
- Bookmark aggregate 테스트 보강 여부 결정
- SearchHistory query/searchWord 호환 범위 기록

작업 2: 추천 알고리즘 테스트
- MmrServiceTest 추가
- LlmRecommendationServiceTest edge-case 확장

작업 3: 추천 application/영속성 테스트
- RecommendationCommandServiceTest 추가
- RecommendationSchedulerTest 추가
- RecommendedPostRepositoryTest / RecommendationHistoryTest 추가

작업 4: 검색 API/순위 edge-case 테스트
- SearchControllerIntegrationTest 추가
- SearchServiceImplTest ranking/fallback edge-case 확장

작업 5: 후속 이벤트 contract 테스트
- TechnicalPostIndexed 이벤트 도입 전 publisher/subscriber contract test 준비
```

---

## 8. 테스트 구조 개선 제안

현재 테스트는 기능별로 잘 나뉘어 있지만 DDD 전환 후에는 다음 구조를 목표로 한다.

```text
src/test/java/com/techfork/domain
  activity
    bookmark
      entity
        BookmarkTest
      repository
        BookmarkRepositoryTest
      service
        BookmarkCommandServiceTest
        BookmarkQueryServiceTest
      integration
        BookmarkIntegrationTest
    readpost
      domain
        ReadPostFirstReadPolicyTest
      entity
        ReadPostTest
      service
        ReadPostCommandServiceTest
        ReadPostQueryServiceTest
      repository
        ReadPostRepositoryTest
      integration
        ReadPostIntegrationTest
    readhistory
      dto
        SearchHistoryRequestTest
      repository
        SearchHistoryRepositoryTest
      service
        ReadHistoryCommandServiceTest
      integration
        SearchHistoryIntegrationTest

  post
    entity
      PostTest
    batch
      PostSummaryProcessorTest
      PostSummaryReaderTest
      PostSummaryWriterTest
      PostEmbeddingProcessorTest
      PostEmbeddingWriterTest
    service
      ContentChunkerServiceTest
      SummaryExtractionServiceTest
      PostQueryServiceTest

  useraccount
    entity
      UserTest
      UserInterestTest
    service
      UserCommandServiceTest
      InterestCommandServiceTest

  personalization
    service
      PersonalizationProfileServiceTest
    scheduler
      PersonalizationProfileSchedulerTest

  recommendation
    entity
      RecommendationHistoryTest
    service
      MmrServiceTest
      LlmRecommendationServiceTest
      RecommendationCommandServiceTest
      RecommendationQueryServiceTest
    scheduler
      RecommendationSchedulerTest

  search
    service
      SearchServiceImplTest
    controller
      SearchControllerIntegrationTest
```

주의:

- evaluation 테스트는 일반 단위/통합 테스트와 분리해서 유지한다.
- 검색 품질 튜닝은 evaluation suite에서 한다.
- 검색 로직 회귀는 `domain/search` 일반 테스트에서 빠르게 잡는다.

---

## 9. 완료 기준

DDD 리팩터링을 본격적으로 진행하기 전 최소 완료 기준은 다음이다.

```text
P0 테스트가 모두 존재한다.
./gradlew test -PexcludeIntegration 통과.
Activity 4.1 후속 정리 후 기존 Activity 테스트 통과.
PostTest로 기술 게시글 애그리거트 기본 규칙 보호.
PersonalizationProfileServiceTest로 Personalization Profile 생성 흐름 보호.
MmrServiceTest와 LlmRecommendationServiceTest로 추천 생성 핵심 흐름 보호.
SearchServiceImplTest로 일반/개인화 검색 회귀 보호.
```

권장 검증 명령:

```bash
./gradlew test -PexcludeIntegration
./gradlew integrationTest
```

대규모 검색/추천 품질 검증이 필요할 때만 실행:

```bash
./gradlew evaluationTest
```

---

## 10. 최종 결론

현재 테스트는 양이 적지는 않지만, DDD 전환 관점에서는 다음이 부족하다.

```text
애그리거트 루트 테스트
개인화 프로필 edge-case/trigger 테스트
추천 생성 테스트
검색 회귀 테스트
임베딩 pipeline 테스트
```

따라서 다음 순서로 보강하는 것이 가장 안전하다.

```text
Activity 4.1 slice 안전망 확인
→ Post 애그리거트 테스트
→ 개인화 프로필 테스트
→ 추천 생성 테스트
→ 검색 회귀 테스트
→ DDD 리팩터링 진행
```

이렇게 하면 테스트를 무작정 많이 작성하는 것이 아니라, **DDD 전환 중 깨질 가능성이 높은 경계부터 보호하는 테스트 전략**을 가져갈 수 있다.
