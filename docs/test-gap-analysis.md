# TechFork 테스트 갭 분석

> 목적: DDD 전환과 테스트 개선 로드맵을 실행하기 전에, 현재 테스트가 어떤 영역을 보호하고 있고 어떤 영역이 비어 있는지 분석한다.  
> 관련 문서: [`docs/ubiquitous-language/README.md`](./ubiquitous-language/README.md), [`docs/ddd-test-refactoring-roadmap.md`](./ddd-test-refactoring-roadmap.md)

## 1. 분석 요약

현재 테스트는 **Controller/Repository/Service 단위의 기존 기능 회귀 테스트는 꽤 많이 쌓여 있지만, DDD 전환에 필요한 애그리거트 단위 테스트와 핵심 개인화/추천/검색 알고리즘 테스트가 부족한 상태**다.

테스트 보강 우선순위는 `docs/domain-strategy.md`의 하위 도메인 분류를 따른다. 즉, **핵심 하위 도메인인 개인화 기술 콘텐츠 발견(Search/Recommendation), 사용자 관심/행동 기반 개인화 모델링(Personalization Profile/Activity), 기술 게시글 이해/풍부화(Post enrichment)** 를 먼저 보호한다.

요약하면 다음과 같다.

| 영역 | 현재 상태 | DDD 전환 관점 평가 |
|---|---|---|
| Activity | 서비스, repository, controller 테스트가 충분한 편 | `ScrabPost → Bookmark` 리팩터링 전 안전망으로 좋음 |
| Source / Ingestion | RSS reader, processor, writer, scheduler, crawling service 테스트가 잘 있음 | 파이프라인 보호 수준 좋음 |
| Post / Content | 조회 API/repository는 강함. 도메인 엔티티/요약/임베딩 테스트는 부족 | `Post = 기술 게시글` 애그리거트 테스트 필요 |
| User Account | 온보딩/관심사/계정 프로필은 강함 | 사용자 계정 애그리거트와 온보딩 흐름은 비교적 잘 보호되어 있다 |
| Personalization Profile | 일반 테스트 안전망이 약함 | `PersonalizationProfileService`/`PersonalizationProfileDocument` 중심 개인화 흐름 보호가 필요하다 |
| Recommendation | 조회 쪽은 일부 있음. 추천 생성/후보 탐색/MMR/이력화 테스트는 부족 | DDD 전환 전 가장 큰 리스크 중 하나 |
| Search | 일반 실행 테스트가 거의 없음. evaluation suite만 존재 | 일반 회귀 테스트 부재가 큼 |
| Auth / Security | 토큰/필터/컨트롤러 테스트는 비교적 강함 | OAuth handler/OIDC 일부 보강 여지 |
| Notification | 엔티티만 있고 테스트/기능이 약함 | 현재는 낮은 우선순위 |
| Admin / Ops | 개발자 토큰 중심 테스트 있음 | 배치 수동 실행 API 커버 보강 필요 |

가장 중요한 갭은 다음 5개다.

1. **SearchServiceImpl 일반 회귀 테스트 부재**
2. **LlmRecommendationService / MmrService 테스트 부재**
3. **Personalization Profile(`PersonalizationProfileService`) 일반 테스트 부재**
4. **Post 애그리거트 단위 테스트 부재**
5. **Post embedding pipeline 테스트 부재**

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
| domain/activity | 6 | 48 | Controller, service, repository 커버 좋음 |
| domain/admin | 1 | 6 | 개발자 토큰 중심 |
| domain/auth | 3 | 30 | AuthService, KakaoOAuthService, AuthController integration |
| domain/post | 4 | 72 | Controller/repository/query service 중심 |
| domain/recommendation | 3 | 8 | 조회/컨버터 중심, 생성 로직 부족 |
| domain/source | 10 | 38 | RSS/배치/스케줄러/락/웹훅 커버 좋음 |
| domain/useraccount + domain/personalization | 8 | 61 | User Account service/controller/repository 커버 + Personalization Profile 기본 unit 안전망 확보 |
| global | 6 | 33 | Security, cache, util, integration base |
| evaluation | 27 | 18 | 검색/추천 품질 평가 및 fixture setup |

### 3.2 untracked 테스트 주의사항

현재 working tree에는 다음 untracked 테스트 디렉터리가 있다.

```text
src/test/java/com/techfork/domain/post/batch/
```

포함 파일:

```text
PostSummaryProcessorTest.java
PostSummaryReaderTest.java
PostSummaryWriterTest.java
```

이 파일들은 현재 Git tracked 상태가 아니므로, 테스트 갭 분석에서는 **작성 중이거나 커밋되지 않은 테스트**로 본다.

의미:

- Post summary pipeline의 일부 갭은 이미 작업 중일 수 있다.
- 하지만 main branch/PR 기준 안전망으로 보려면 tracked 상태로 포함되어야 한다.
- 이 문서에서는 “untracked 보강 후보”로 별도 표기한다.

---

## 4. 컨텍스트별 상세 분석

### 4.1 Activity 컨텍스트

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `ActivityCommandServiceTest` | unit/mock | 읽기 저장, 첫 읽기 조회수 증가, 검색 기록 저장, 북마크 추가/삭제, 예외 |
| `ActivityQueryServiceTest` | unit/mock | 북마크 목록, 읽은 게시글 목록, 키워드/북마크 여부 조합 |
| `ReadPostRepositoryTest` | JPA | 최근 읽은 글, 중복 읽기 저장, 커서 조회/중복 제거 |
| `ScrabPostRepositoryTest` | JPA | 북마크 커서 조회, 북마크된 postId 조회 |
| `SearchHistoryRepositoryTest` | JPA | 최근 검색 기록 조회 |
| `ActivityControllerIntegrationTest` | integration | Activity API 전체 흐름 |

#### 평가

Activity는 현재 가장 보호가 잘 된 영역 중 하나다.  
DDD 리팩터링 1순위인 `ScrabPost → Bookmark` 전환을 시작하기에 비교적 안전하다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P0 | `ScrabPost` 명칭을 `Bookmark`로 바꾼 뒤 동일 테스트 유지/rename | 용어 리팩터링 중 회귀 방지 |
| P1 | `ReadPost`, `Bookmark`, `SearchHistory` 도메인 엔티티 단위 테스트 | 애그리거트/record aggregate 관점의 테스트 명확화 |
| P1 | 북마크 DB 테이블명 legacy 유지/변경 정책 테스트 | `@Table(name = "scrap_posts")` 유지 시 의도 명시 필요 |

#### 추천 다음 작업

```text
1. Activity 기존 테스트 전체 통과 확인
2. ScrabPostRepositoryTest → BookmarkRepositoryTest로 rename 계획 수립
3. ScrabPost → Bookmark 리팩터링
4. 기존 Activity 테스트가 전부 통과하는지 확인
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
| `PostSummaryProcessorTest` | untracked | 요약/키워드 반영 후보 |
| `PostSummaryReaderTest` | untracked | lazy load 후보 |
| `PostSummaryWriterTest` | untracked | summary/keyword JDBC binding 후보 |

#### 평가

Post 조회 API와 repository는 강하다.  
하지만 DDD 전환 관점에서 필요한 **`Post` 애그리거트 단위 테스트**가 없다.  
또한 검색/추천의 핵심 입력인 **embedding pipeline**이 거의 보호되지 않는다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P0 | `PostTest` | `Post = 기술 게시글` 애그리거트 루트의 기본 불변식 보호 필요 |
| P0 | `PostEmbeddingProcessorTest` | 제목/요약/청크 임베딩으로 `PostDocument` 생성하는 핵심 pipeline 보호 필요 |
| P0 | `PostEmbeddingWriterTest` | Elasticsearch bulk index + `embeddedAt` update 회귀 위험 큼 |
| P1 | `ContentChunkerServiceTest` | semantic search 품질에 직접 영향 |
| P1 | `PostDocumentTest` | publishedAt serialization, embedding/chunk projection 보호 |
| P1 | `SummaryExtractionServiceTest` | LLM 응답 파싱/실패 처리 보호 필요 |
| P1 | untracked `PostSummary*Test` tracked 반영 여부 결정 | 현재 작성 중 테스트를 공식 안전망에 포함할지 판단 필요 |
| P2 | `PostKeywordRepositoryTest` | 키워드 bulk 조회가 여러 서비스 응답 조합에 쓰임 |

#### 추천 추가 테스트

```text
PostTest
- RssFeedItem으로 기술 게시글을 생성한다.
- 생성 시 company는 TechBlog.companyName 또는 RssFeedItem.company 스냅샷을 가진다.
- 요약과 짧은 요약을 갱신한다.
- 게시글 키워드를 추가한다.
- 게시글 키워드를 초기화한다.
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
| `InterestCommandServiceTest` | unit/mock | 관심사 저장, 기존 관심사 clear, invalid keyword category |
| `UserCommandServiceTest` | unit/mock | 온보딩, 계정 프로필 수정, 탈퇴 |
| `UserQueryServiceTest` | unit/mock | 계정 프로필 조회 |
| `evaluation/search/setup/PersonalizationProfileServiceTest` | evaluation-setup | 테스트 사용자 개인화 프로필 생성용 setup |

#### 평가

- **User Account 쪽**은 온보딩, 관심사, 계정 프로필, 탈퇴 흐름이 비교적 잘 보호되어 있다.
- 기존에는 **Personalization Profile 쪽** 일반 테스트 lane이 비어 있었지만, 이제 `PersonalizationProfileServiceTest` 기본 안전망이 추가되었다.
- evaluation setup용 `PersonalizationProfileServiceTest`는 여전히 별도 목적이므로, 일반 unit test lane과 구분해서 본다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| 완료 | `PersonalizationProfileServiceTest` 일반 단위 테스트 | 활동 데이터 수집, 파싱, fallback, 저장, 추천 실패 격리 기본 흐름 보호 완료 |
| P0 | LLM 응답 parsing 테스트 | `### PROFILE`, `### KEYWORDS` parsing 실패 시 품질/장애 영향 |
| P0 | 관심사 변경 후 개인화 프로필 생성 트리거 검증 | `UserInterestsChanged` 이벤트 도입 전 현재 동작 보호 |
| P1 | `UserTest` | User Account aggregate의 소셜 사용자 생성, 온보딩 ACTIVE, 탈퇴 anonymization, reactivate 규칙 보호 |
| P1 | `UserInterestCategory/UserInterestKeyword` 도메인 테스트 | 관심 키워드가 카테고리에 속해야 한다는 규칙 명시 |
| P1 | `PersonalizationProfileSchedulerTest` | 매일 06:00 KST active user personalization profile regeneration 보호 |
| P2 | `InterestQueryServiceTest` | 관심사 조회 변환 로직 보호 |

#### 추천 추가 테스트

```text
PersonalizationProfileServiceTest
- 관심사, 읽은 게시글, 북마크, 검색 기록을 활동 데이터로 수집한다.
- 읽은 시간은 읽기 몰입도 자연어로 변환된다.
- LLM 응답에서 profileText와 keyKeywords를 파싱한다.
- 파싱 실패 시 fallback 정책을 따른다.
- profileText를 임베딩하여 PersonalizationProfileDocument를 저장한다.
- 개인화 프로필 생성 후 추천 생성을 호출한다.
- 추천 생성 실패가 개인화 프로필 저장을 깨뜨리지 않는지 정책을 검증한다.
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
| evaluation recommendation tests | evaluation | K, lambda, MMR candidate size, title/summary ratio 품질 비교 |

#### 평가

조회 쪽은 일부 보호되어 있지만, 핵심인 **추천 생성 로직이 거의 비어 있다.**  
`LlmRecommendationService`는 Personalization Profile(`PersonalizationProfileDocument`), Elasticsearch, Post, Activity, RRF, MMR, time decay, history 저장을 모두 다루므로 DDD 리팩터링 전 반드시 테스트가 필요하다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P0 | `MmrServiceTest` | 추천 다양성/관련성 알고리즘의 핵심 |
| P0 | `LlmRecommendationServiceTest` | 추천 생성, 읽은 글 제외, RRF, 기존 추천 이력화, 새 추천 저장 보호 |
| P0 | 추천 생성 실패/빈 후보/프로필 없음 정책 테스트 | 장애 시 사용자 경험과 batch 안정성에 중요 |
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
- 읽은 게시글은 후보 검색 filter에 포함된다.
- vectorHits와 keywordHits를 RRF로 합친다.
- time decay가 similarityScore에 반영된다.
- 기존 추천은 RecommendationHistory로 저장된다.
- 기존 추천은 삭제되고 새 추천이 저장된다.
- Elasticsearch 검색 실패 시 정책대로 empty/failure 처리한다.
```

---

### 4.6 Search 컨텍스트

#### 현재 테스트

일반 domain/search 테스트 디렉터리는 없다.

```text
src/test/java/com/techfork/domain/search  // 없음
```

대신 evaluation suite가 있다.

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `SearchPhase1FieldWeightTest` | evaluation | field weight 품질 비교 |
| `SearchPhase2FieldWeightDetailTest` | evaluation | field weight 세부 조정 |
| `SearchPhase3ChunkStructureTest` | evaluation | chunk 구조 품질 비교 |
| `SearchPhase4KnnParameterTest` | evaluation | KNN parameter 품질/latency 비교 |
| `SearchPhase5QueryStructureTest` | evaluation | bool vs dis_max 구조 비교 |

#### 평가

Search는 현재 **품질 평가 테스트는 있지만 일반 회귀 테스트가 없다.**  
evaluation suite는 무겁고 runtime/profile/fixture 의존이 강하므로 DDD 리팩터링 안전망으로 쓰기 어렵다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P0 | `SearchServiceImplTest` 일반 단위 테스트 | 일반 검색/개인화 검색 핵심 흐름 보호 필요 |
| P0 | RRF 결합 테스트 | 검색 결과 순위 품질과 직접 연결 |
| P0 | 개인화 fallback/reranking 테스트 | Personalization Profile(`PersonalizationProfileDocument`) 경계 정리 전 필요 |
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
| `AuthServiceTest` | unit/mock | refresh, logout, developer token, kakao login |
| `KakaoOAuthServiceTest` | unit/mock | Kakao user info success/failure |
| `AuthControllerIntegrationTest` | integration | refresh/logout/kakao login API |
| `SecurityIntegrationTest` | integration | 인증/인가, 토큰 오류, 권한, 탈퇴 사용자 |
| `JwtAuthenticationFilterTest` | unit/mock | access token filter, cache hit/miss, invalid token |
| `UserAuthCacheServiceTest` | unit/mock | auth cache serialization/deserialization |

#### 평가

Auth/Security는 비교적 안정적이다.  
DDD 전환의 핵심 경로는 아니지만, User 컨텍스트 리팩터링 시 인증 모델이 깨지지 않도록 유지해야 한다.

#### 남은 갭

| 우선순위 | 갭 | 이유 |
|---|---|---|
| P1 | Apple/OIDC success/failure handler 테스트 | 실제 인증 표면은 `global/security`에 있으므로 회귀 보호 필요 |
| P1 | withdraw/reactivate 이후 auth cache/token 정책 테스트 | User 상태 전환과 보안 정책 연결 |
| P2 | Cookie OAuth2 authorization request repository 테스트 | OAuth edge case 보호 |

---

### 4.8 Admin / Ops 컨텍스트

#### 현재 테스트

| 테스트 | 성격 | 주요 커버 |
|---|---|---|
| `AdminControllerIntegrationTest` | integration | developer token 생성, 권한/인증 실패 |
| Source tests | unit/integration | crawling service, job, scheduler, webhook |

#### 평가

AdminController의 개발자 토큰 API는 테스트되어 있다.  
하지만 수동 배치 실행 API는 상대적으로 약하다.

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
| `Post` | 직접 `PostTest` 없음 | 기술 게시글 생성/요약/키워드/조회수 테스트 필요 |
| `PostKeyword` | 직접 테스트 없음 | `Post` 내부 엔티티로 테스트하면 충분 |
| `User` | `UserCommandServiceTest` 중심 | User Account aggregate 관점의 직접 `UserTest` 필요 |
| `UserInterestCategory/Keyword` | repository/service 중심 | User Account 도메인 규칙 테스트 보강 필요 |
| `PersonalizationProfileDocument` | evaluation setup 중심 | Personalization Profile projection 생성/파싱 일반 테스트 필요 |
| `ReadPost` | service/repository 중심 | record aggregate 단위 테스트는 선택 |
| `Bookmark` | 현재 `ScrabPost` service/repository 중심 | rename 후 `Bookmark` 단위 테스트 필요 |
| `SearchHistory` | repository/service 중심 | record aggregate 단위 테스트는 선택 |
| `RecommendedPost` | query/controller 중심 | 생성/순위/unique 정책 보강 필요 |
| `RecommendationHistory` | 없음 | 이력화/fromRecommendedPost/click 테스트 필요 |
| `RecommendationSet` 개념 | 없음 | DDD 모델 도입 시 신규 테스트 필요 |

---

## 6. 우선순위별 테스트 백로그

### P0. DDD 전환 전 반드시 보강

| 테스트 | 목적 | 선행/연결 작업 |
|---|---|---|
| `PostTest` | `Post = 기술 게시글` 애그리거트 보호 | Post 용어 정리, EDifficultyLevel 제거 |
| `PersonalizationProfileServiceTest` | Personalization Profile 생성 흐름 보호 | User Account / Personalization Profile 경계 정리 |
| `MmrServiceTest` | 추천 알고리즘 핵심 보호 | Recommendation DDD 전환 |
| `LlmRecommendationServiceTest` | 추천 생성/이력화/읽은 글 제외 보호 | `PersonalizedProfileGenerated` 이벤트 도입 전 |
| `SearchServiceImplTest` | 검색 일반 회귀 안전망 | Search read model/adapter 분리 전 |
| `PostEmbeddingProcessorTest` | 임베딩 문서 생성 보호 | `TechnicalPostIndexed` 이벤트 도입 전 |
| `PostEmbeddingWriterTest` | ES 색인 + embeddedAt update 보호 | embedding pipeline 리팩터링 전 |
| `Bookmark` rename 후 Activity 테스트 유지 | 용어 리팩터링 회귀 보호 | `ScrabPost → Bookmark` |

### P1. DDD 전환 중 보강

| 테스트 | 목적 |
|---|---|
| `ContentChunkerServiceTest` | semantic search 품질 입력 보호 |
| `SummaryExtractionServiceTest` | LLM 요약 응답 parsing 보호 |
| `UserTest` | 사용자 애그리거트 상태 전이 보호 |
| `RecommendationCommandServiceTest` | 추천 재생성 use case 보호 |
| `RecommendationSchedulerTest` | 일일 추천 생성 스케줄 보호 |
| `RecommendedPostRepositoryTest` | rank order/delete/unique 보호 |
| `RecommendationHistoryTest` | 이력화/click 기록 보호 |
| `PersonalizationProfileSchedulerTest` | 일일 개인화 프로필 재생성 보호 |
| `SearchControllerIntegrationTest` | 검색 API contract 보호 |
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
1. 현재 Activity 테스트 통과 확인
2. ScrabPost → Bookmark rename 전후 테스트 안정화
3. PostTest 작성
4. Post embedding pipeline 테스트 작성
5. PersonalizationProfileServiceTest 작성
6. MmrServiceTest 작성
7. LlmRecommendationServiceTest 작성
8. SearchServiceImplTest 작성
9. RecommendationCommand/Scheduler/History 테스트 보강
10. Source 이벤트 contract 테스트 준비
```

가장 먼저 시작할 실제 작업 단위는 다음이 좋다.

```text
작업 1: Activity/Bookmark 리팩터링 안전망
- 기존 ActivityCommandServiceTest 확인
- 기존 ActivityControllerIntegrationTest 확인
- ScrabPostRepositoryTest를 Bookmark 기준으로 rename할 계획 수립

작업 2: Post 애그리거트 테스트
- PostTest 추가
- EDifficultyLevel 제거 후 문서 정리 및 회귀 확인

작업 3: Personalization Profile 테스트
- PersonalizationProfileServiceTest 추가
- LLM/Embedding/RecommendationService는 mock 처리

작업 4: 추천 생성 테스트
- MmrServiceTest 추가
- LlmRecommendationServiceTest 추가

작업 5: 검색 회귀 테스트
- SearchServiceImplTest 추가
- evaluation suite와 별개로 빠른 단위 테스트 구성
```

---

## 8. 테스트 구조 개선 제안

현재 테스트는 기능별로 잘 나뉘어 있지만 DDD 전환 후에는 다음 구조를 목표로 한다.

```text
src/test/java/com/techfork/domain
  activity
    entity 또는 model
      BookmarkTest
      ReadPostTest
    service
      ActivityCommandServiceTest
      ActivityQueryServiceTest
    repository
      BookmarkRepositoryTest
      ReadPostRepositoryTest
      SearchHistoryRepositoryTest
    controller
      ActivityControllerIntegrationTest

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

  user
    entity
      UserTest
      UserInterestTest
    service
      UserCommandServiceTest
      InterestCommandServiceTest
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
Activity/Bookmark rename 후 기존 Activity 테스트 통과.
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
개인화 프로필 테스트
추천 생성 테스트
검색 회귀 테스트
임베딩 pipeline 테스트
```

따라서 다음 순서로 보강하는 것이 가장 안전하다.

```text
Activity/Bookmark 안전망 확인
→ Post 애그리거트 테스트
→ 개인화 프로필 테스트
→ 추천 생성 테스트
→ 검색 회귀 테스트
→ DDD 리팩터링 진행
```

이렇게 하면 테스트를 무작정 많이 작성하는 것이 아니라, **DDD 전환 중 깨질 가능성이 높은 경계부터 보호하는 테스트 전략**을 가져갈 수 있다.
