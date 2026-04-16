# TechFork 유비쿼터스 언어 사전

> 팀이 코드·PR·기획 문서에서 공통으로 사용하는 표준 용어를 정의한 사전입니다.  
> 코드 네이밍, PR 리뷰, 기획 문서 작성 시 매일 참조합니다.  
> 관련 문서: [도메인 전략 설계](domain-strategy.md) | [전술 설계](tactical-design.md)

---

## 1. Source / Ingestion

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 기술 블로그 | `TechBlog` | 수집 대상이 되는 회사/조직의 기술 블로그 소스. `companyName`, `blogUrl`, `rssUrl`, `logoUrl`, `lastCrawledAt`을 가진다. |
| RSS 피드 | `rssUrl`, `RssFeedReader` | 기술 블로그에서 게시글 목록을 가져오는 외부 피드. |
| 피드 아이템 | `RssFeedItem` | RSS 엔트리를 내부 기술 게시글 후보로 변환한 값 객체 성격의 DTO. 제목, URL, 본문, 정제 본문, 발행일, 회사명, 썸네일 등을 가진다. |
| 크롤링 | `CrawlingService`, `rssCrawlingJob` | 모든 기술 블로그의 RSS를 수집해 신규 기술 게시글을 저장하는 작업. |
| 신규 기술 게시글 판별 | `findExistingUrls`, URL dedup | 이미 저장된 URL은 제외하고 신규 URL만 기술 게시글로 저장하는 정책. |
| 크롤링 잡 | `rssCrawlingJob` | `fetchAndSaveRssStep → extractSummaryStep → embedAndIndexStep` 순서의 전체 파이프라인. |
| 요약·임베딩 잡 | `summaryAndEmbeddingJob` | 이미 저장된 기술 게시글에 대해 요약/임베딩만 수행하는 별도 배치. |
| 크롤링 실패 알림 | `WebhookNotificationService` | 크롤링 실패 시 Discord Webhook으로 운영 알림을 전송하는 개념. |

주요 근거 파일:

- `src/main/java/com/techfork/domain/source/entity/TechBlog.java`
- `src/main/java/com/techfork/domain/source/dto/RssFeedItem.java`
- `src/main/java/com/techfork/domain/source/batch/RssFeedReader.java`
- `src/main/java/com/techfork/domain/source/config/RssCrawlingJobConfig.java`
- `src/main/java/com/techfork/domain/source/service/CrawlingService.java`

---

## 2. Post / Content

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 기술 게시글 | `Post` | TechFork가 다루는 핵심 콘텐츠. 외부 기술 블로그 RSS 피드 아이템에서 생성된다. |
| 제목 | `title` | 기술 게시글 제목. 검색/임베딩의 주요 대상. |
| 원문 본문 | `fullContent` | RSS에서 가져온 HTML/마크다운 포함 본문. |
| 정제 본문 | `plainContent` | HTML/마크다운을 제거한 검색/요약용 텍스트. |
| 요약 | `summary` | LLM이 생성한 상세 요약. |
| 짧은 요약 | `shortSummary` | 목록/카드 UI에 적합한 축약 요약. |
| 게시글 키워드 | `PostKeyword` | LLM 요약 과정에서 추출된 기술 게시글 대표 키워드. |
| 발행일 | `publishedAt` | 외부 기술 블로그에서 게시글이 발행된 시각. |
| 수집일 | `crawledAt` | TechFork가 해당 기술 게시글을 수집한 시각. |
| 임베딩 완료일 | `embeddedAt` | 기술 게시글이 임베딩되어 Elasticsearch에 색인된 시각. |
| 조회수 | `viewCount` | 사용자가 처음 읽은 경우 증가하는 기술 게시글 popularity 지표. |
| 검색 문서 | `PostDocument` | Elasticsearch `posts` 인덱스에 저장되는 기술 게시글 projection. |
| 콘텐츠 청크 | `ContentChunk` | 긴 본문을 임베딩 검색용으로 분할한 단위. `chunkOrder`, `chunkText`, `embedding`을 가진다. |
| 출처명 | `Post.company`, `TechBlog.companyName` | 기술 게시글이 어느 기술 블로그/회사에서 왔는지 표시하기 위한 이름. `Post.company`는 `TechBlog.companyName`의 비정규화 스냅샷으로 본다. |
| 정렬 기준 | `EPostSortType` | `LATEST` 최신순, `POPULAR` 인기순. |

주요 근거 파일:

- `src/main/java/com/techfork/domain/post/entity/Post.java`
- `src/main/java/com/techfork/domain/post/entity/PostKeyword.java`
- `src/main/java/com/techfork/domain/post/document/PostDocument.java`
- `src/main/java/com/techfork/domain/post/document/ContentChunk.java`
- `src/main/java/com/techfork/domain/post/batch/PostSummaryProcessor.java`
- `src/main/java/com/techfork/domain/post/batch/PostEmbeddingProcessor.java`

---

## 3. User / Profile

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 사용자 | `User` | 서비스를 사용하는 주체. 소셜 로그인 기반으로 생성된다. |
| 소셜 사용자 | `createSocialUser`, `SocialType` | Kakao/Apple 등 외부 인증 제공자로부터 생성된 사용자. |
| 소셜 타입 | `SocialType` | `KAKAO`, `APPLE`. |
| 회원 상태 | `UserStatus` | `PENDING`, `ACTIVE`, `WITHDRAWN`. |
| 대기 사용자 | `PENDING` | 온보딩 미완료 상태. |
| 활성 사용자 | `ACTIVE` | 온보딩 완료 상태. |
| 탈퇴 사용자 | `WITHDRAWN` | 탈퇴 완료 상태. 개인정보는 null 처리된다. |
| 온보딩 | `completeOnboarding` | 닉네임, 이메일, 설명, 관심사를 저장하고 사용자를 활성화하는 절차. |
| 관심 카테고리 | `EInterestCategory`, `UserInterestCategory` | 사용자가 선택한 기술 분야. 예: Backend, Frontend, AI/ML, DevOps, Architecture 등. |
| 관심 키워드 | `EInterestKeyword`, `UserInterestKeyword` | 카테고리에 속한 구체 기술 키워드. 예: Spring, React, Kubernetes, DDD 등. |
| 계정 프로필 | `nickName`, `description`, `profileImage` | 사용자에게 보이는 기본 프로필 정보. |
| 개인화 프로필 | `UserProfileDocument` | 사용자 활동 데이터를 LLM으로 요약하고 임베딩한 개인화용 프로필 문서. |
| 프로필 텍스트 | `profileText` | 검색 리랭킹과 추천에 사용할 사용자 관심사 설명문. |
| 프로필 벡터 | `profileVector` | `profileText`를 임베딩한 벡터. 개인화 검색/추천의 핵심 입력. |
| 핵심 키워드 | `keyKeywords` | LLM이 사용자 활동에서 추출한 3~5개 대표 관심 키워드. 추천 BM25 검색에도 사용된다. |
| 활동 데이터 | `UserActivityData` | 관심사, 최근 읽은 기술 게시글, 북마크한 기술 게시글, 검색 기록을 합친 사용자 분석 입력. |

주요 근거 파일:

- `src/main/java/com/techfork/domain/user/entity/User.java`
- `src/main/java/com/techfork/domain/user/enums/UserStatus.java`
- `src/main/java/com/techfork/domain/user/enums/EInterestCategory.java`
- `src/main/java/com/techfork/domain/user/enums/EInterestKeyword.java`
- `src/main/java/com/techfork/domain/user/service/InterestCommandService.java`
- `src/main/java/com/techfork/domain/user/service/UserProfileService.java`
- `src/main/java/com/techfork/domain/user/document/UserProfileDocument.java`

---

## 4. Activity

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 읽은 게시글 | `ReadPost` | 사용자가 기술 게시글을 읽은 기록. `readAt`, `readDurationSeconds`를 가진다. |
| 읽은 시간 | `readDurationSeconds` | 사용자가 기술 게시글을 읽은 지속 시간. 사용자 프로필 생성 시 읽기 몰입도로 변환된다. |
| 읽기 몰입도 | `convertReadingDurationToNaturalLanguage` | 읽은 시간을 `가볍게 훑어봄`, `빠르게 읽음`, `읽음`, `정독함`, `깊게 읽음`으로 해석한 값. |
| 첫 읽기 | `isFirstRead` | 사용자가 특정 기술 게시글을 처음 읽은 경우. 이때만 조회수가 증가한다. |
| 검색 기록 | `SearchHistory` | 사용자가 입력한 검색어와 검색 시각. |
| 검색어 | `SearchQuery`, 현재 `searchWord` | 검색 기록에 저장되는 사용자 입력. 코드에서는 `searchWord`와 `query`가 혼재하지만 표준 용어는 검색어/SearchQuery로 통일한다. |
| 북마크 | `Bookmark`, 현재 `ScrabPost` | 사용자가 기술 게시글을 저장하는 행위. API/제품 용어 기준으로 북마크로 통일한다. 현재 엔티티명 `ScrabPost`와 DB 테이블 `scrap_posts`는 legacy 이름이다. |
| 북마크 여부 | `isBookmarked` | 기술 게시글 목록/검색/추천 응답에 붙는 사용자별 저장 여부. |

현재는 사용자 프로필 생성의 입력으로 사용되며, 향후 추천/검색 품질 개선을 위한 독립적 분석 도메인으로 확장 가능성이 있어 별도 컨텍스트로 분리한다.

주요 근거 파일:

- `src/main/java/com/techfork/domain/activity/entity/ReadPost.java`
- `src/main/java/com/techfork/domain/activity/entity/SearchHistory.java`
- `src/main/java/com/techfork/domain/activity/entity/ScrabPost.java`
- `src/main/java/com/techfork/domain/activity/service/ActivityCommandService.java`
- `src/main/java/com/techfork/domain/activity/service/ActivityQueryService.java`

---

## 5. Search

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 검색어 / SearchQuery | `query`, 현재 `searchWord` | 사용자가 찾고 싶은 기술 주제/키워드. 코드에서는 `query`, `searchWord`가 혼재하지만 표준 용어는 검색어/SearchQuery로 통일한다. |
| BM25 검색 / 어휘 검색 | `searchOnlyBm25`, `performLexicalSearch` | 제목/요약/본문 청크 텍스트 기반 검색. |
| 시맨틱 검색 | `searchOnlySemantic`, `performSemanticSearch` | 검색어 임베딩과 기술 게시글 임베딩의 k-NN 기반 검색. |
| 하이브리드 검색 | `performHybridSearch` | BM25 검색과 시맨틱 검색을 병렬 실행한 뒤 결합하는 검색. |
| RRF | `RrfScorer` | BM25 결과와 시맨틱 결과의 순위를 결합하는 Reciprocal Rank Fusion. |
| 일반 검색 | `searchGeneral` | 사용자 프로필 없이 하이브리드 검색 결과를 반환하는 검색. |
| 개인화 검색 | `searchPersonalized` | 사용자 프로필 벡터가 있으면 검색 결과를 개인화 리랭킹하는 검색. |
| 개인화 리랭킹 | `personalReranking` | 사용자 프로필 벡터와 기술 게시글 제목/요약 벡터의 유사도를 반영해 재정렬하는 과정. |
| 검색 결과 | `SearchResult` | 검색 응답 단위. 기술 게시글 정보, 점수, 조회수, 북마크 여부를 포함한다. |
| 하이브리드 점수 | `hybridScore` | RRF로 결합된 검색 점수. |
| 개인화 점수 | `personalScore` | 사용자 프로필과 검색 결과 문서 간 유사도 점수. |
| 최종 점수 | `finalScore` | 하이브리드 점수와 개인화 점수를 조합한 최종 정렬 점수. |
| 프로필 없음 fallback | `Personalized Search [FALLBACK]` | 사용자 프로필이 없을 때 개인화 없이 일반 하이브리드 결과를 반환하는 흐름. |

주요 근거 파일:

- `src/main/java/com/techfork/domain/search/service/SearchService.java`
- `src/main/java/com/techfork/domain/search/service/SearchServiceImpl.java`
- `src/main/java/com/techfork/domain/search/dto/SearchResult.java`
- `src/main/java/com/techfork/domain/search/config/GeneralSearchProperties.java`

---

## 6. Recommendation

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 추천 대상 사용자 | `User user` | 추천을 생성할 활성 사용자. |
| 추천 후보군 | `MmrCandidate` | 사용자 프로필 벡터로 검색한 기술 게시글 후보. |
| 추천 게시글 | `RecommendedPost` | 현재 사용자에게 노출할 추천 결과. |
| 추천 목록 | `RecommendationSet` (표준 확정), 현재 코드: `List<RecommendedPost>` | 사용자별 현재 추천 게시글 묶음. 표준 용어는 `RecommendationSet`으로 확정한다. 현재 코드에는 명시적 루트가 없으며 `RecommendedPost` 단건 구조가 리팩터링 대상이다 (§9.7 참조). |
| 추천 이력 | `RecommendationHistory` | 과거 추천 결과를 보관한 기록. 기존 추천이 새 추천으로 교체될 때 history로 이동한다. |
| 유사도 점수 | `similarityScore` | 사용자 프로필과 게시글 후보 간 관련성 점수. RRF와 시간 감쇠가 반영된다. |
| MMR 점수 | `mmrScore` | 관련성과 다양성을 함께 고려한 추천 선택 점수. |
| 추천 순위 | `rankOrder`, `rank` | 사용자에게 보여줄 추천 순서. |
| 추천 생성 시각 | `recommendedAt` | 추천이 생성된 시각. |
| 추천 클릭 | `isClicked`, `clickedAt`, `markAsClicked` | 추천 이력에서 클릭 여부를 기록하는 개념. 현재 실제 호출 경로는 약해 보인다. **코드 오타 수정 필요**: 현재 `markAsisClicked` → `markAsClicked`. |
| 읽은 게시글 제외 | `createExcludeFilter(readPostIds)` | 사용자가 이미 읽은 기술 게시글은 추천 후보에서 제외하는 정책. |
| 시간 감쇠 | `TimeDecayStrategy` | 발행일에 따라 추천 후보 점수에 가중치를 부여하는 전략. |
| 일일 추천 생성 | `RecommendationScheduler` | 매일 07:00 KST 활성 사용자 대상으로 추천 생성. |
| 추천 재생성 | `regenerateRecommendations` | 사용자가 요청하거나 프로필 갱신 후 추천을 다시 생성하는 행위. |

주요 근거 파일:

- `src/main/java/com/techfork/domain/recommendation/service/LlmRecommendationService.java`
- `src/main/java/com/techfork/domain/recommendation/service/MmrService.java`
- `src/main/java/com/techfork/domain/recommendation/entity/RecommendedPost.java`
- `src/main/java/com/techfork/domain/recommendation/entity/RecommendationHistory.java`
- `src/main/java/com/techfork/domain/recommendation/scheduler/RecommendationScheduler.java`

---

## 7. Auth / Security

| 용어 | 코드상 표현                   | 정의 |
|---|--------------------------|---|
| 소셜 로그인 | `kakaoLogin`, OAuth2/OIDC | Kakao/Apple 계정을 통해 사용자를 식별하고 로그인하는 절차. |
| 액세스 토큰 | `accessToken`            | API 인증용 JWT. |
| 리프레시 토큰 | `refreshToken`           | 액세스 토큰 재발급용 토큰. Cookie와 Redis 저장소를 사용한다. |
| 토큰 갱신 | `refreshToken` API       | 리프레시 토큰을 검증하고 새 액세스/리프레시 토큰을 발급하는 행위. |
| 로그아웃 | `logout`                 | 리프레시 토큰을 삭제하고 쿠키를 제거하는 행위. |
| 개발자 토큰 | `DeveloperTokenResponse` | 관리자 API에서 발급하는 장기 액세스 토큰 성격의 토큰. |
| 사용자 주체 | `UserPrincipal`          | Spring Security 인증 컨텍스트에서 사용자를 나타내는 객체. |

주요 근거 파일:

- `src/main/java/com/techfork/domain/auth/service/AuthService.java`
- `src/main/java/com/techfork/domain/auth/controller/AuthController.java`
- `src/main/java/com/techfork/global/security/jwt/JwtUtil.java`
- `src/main/java/com/techfork/global/security/auth/service/RefreshTokenService.java`
- `src/main/java/com/techfork/global/security/oauth/UserPrincipal.java`

---

## 8. Notification / Admin / Ops

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 알림 토큰 | `NotificationToken` | 사용자에게 푸시/알림을 보낼 수 있는 토큰. 현재는 `token`, `isActive`, `user`만 존재한다. |
| 관리자 배치 실행 | `AdminController` | 요약/임베딩 배치, RSS 크롤링을 수동 실행하는 운영 기능. |
| Webhook 알림 | `WebhookNotificationService` | 크롤링 실패 같은 운영 이벤트를 외부 채널로 전송하는 기능. |

---

## 9. 용어 정합성 결정사항

### 9.1 북마크로 통일

결정: 제품/API 언어 기준으로 **북마크**로 통일한다.

현재 같은 개념이 세 가지 이름으로 섞여 있다.

- 엔티티: `ScrabPost`
- DB 테이블: `scrap_posts`
- API/DTO/에러코드: `Bookmark`, `BOOKMARK_ALREADY_EXISTS`

표준 용어:

- `Bookmark`
- `BookmarkedPost`
- `BookmarkRepository`
- `bookmarkId`
- `bookmarkedAt`

마이그레이션 방향:

1. 문서와 API 용어는 즉시 북마크로 통일한다.
2. 코드에서는 `ScrabPost`를 `Bookmark` 또는 `BookmarkedPost`로 변경한다.
3. DB 테이블 `scrap_posts`는 운영 마이그레이션 비용을 고려해 유지할 수 있지만, 가능하면 `bookmarks`로 rename한다.
4. 유지한다면 테이블 주석/마이그레이션 문서에 legacy naming임을 남긴다.

---

### 9.2 `TechBlog`를 출처 루트로 유지하고 `Post.company`는 비정규화 스냅샷으로 본다

결정: 별도 `Company` 애그리거트는 현재 도입하지 않고, `TechBlog`를 RSS 소스/출처 루트로 유지한다.

정리:

- `TechBlog`: RSS 소스 단위 애그리거트 루트
- `TechBlog.companyName`: 기술 블로그 표시명/출처명
- `Post.company`: 조회, 정렬, 검색 응답 편의를 위해 `TechBlog.companyName`을 복사해 둔 비정규화 필드

주의:

- `Post.company`는 진실의 원천이 아니라 스냅샷이다.
- 출처명이 바뀌어도 과거 게시글의 표시명을 같이 바꿀지, 발행 당시 이름으로 보존할지는 정책 결정이 필요하다.
- 한 회사가 여러 기술 블로그를 운영하는 요구가 생기기 전까지는 `Company` 분리를 보류한다.

---

### 9.3 `Post`는 도메인 문서에서 기술 게시글로 설명한다

결정: 코드상 클래스명 `Post`는 유지하되, 제품/도메인 문서에서는 **기술 게시글**로 설명한다.

이유:

- TechFork의 `Post`는 일반 게시글이 아니라 외부 기술 블로그에서 수집된 기술 콘텐츠다.
- API가 `/posts`이므로 코드 레벨 명칭을 당장 바꾸는 비용은 크다.
- 문서와 기획 언어에서 "기술 게시글"이라고 부르면 도메인 의미가 더 명확하다.

---

### 9.4 검색어, 핵심 키워드, 게시글 키워드 구분

결정: 다음 용어로 구분한다.

| 표준 용어 | 코드상 현재 표현 | 의미 |
|---|---|---|
| 검색어 / SearchQuery | `query`, `searchWord` | 사용자가 직접 입력한 검색 문자열 |
| 핵심 키워드 / KeyKeyword | `keyKeywords` | 개인화 프로필에서 추출한 대표 관심 키워드 |
| 게시글 키워드 / PostKeyword | `PostKeyword.keyword` | 기술 게시글 요약 과정에서 추출된 대표 키워드 |

마이그레이션 방향:

- `SearchHistory.searchWord`는 장기적으로 `searchQuery` 또는 `query`로 변경한다.
- 추천의 `keyKeywords`는 "검색어"라고 부르지 않는다.
- `PostKeyword`는 게시글 메타데이터로 유지한다.

---

### 9.5 계정 프로필과 개인화 프로필을 분리한다

결정: "프로필"을 다음 두 개념으로 분리한다.

| 표준 용어 | 코드상 표현 | 의미 |
|---|---|---|
| 계정 프로필 | `User.nickName`, `User.description`, `User.profileImage` | 사용자에게 보이는 기본 프로필 정보 |
| 개인화 프로필 | `UserProfileDocument.profileText`, `profileVector` | 검색 리랭킹과 추천에 쓰이는 활동 기반 LLM/임베딩 프로필 |

주의:

- `UserProfileDocument`는 사용자 애그리거트 내부 상태라기보다 개인화 검색/추천용 projection이다.
- API/문서에서 "프로필 수정"은 계정 프로필 수정인지, 개인화 프로필 재생성인지 명확히 구분해야 한다.

---

### 9.6 `EDifficultyLevel` 제거 후보

결정: 현재 사용처가 약하므로 **제거 후보**로 본다.

- `BEGINNER`, `INTERMEDIATE`, `ADVANCED`가 정의되어 있지만 현재 핵심 흐름에서 쓰이지 않는다.
- 기획상 난이도 필터/추천 정책이 없다면 제거한다.
- 난이도가 필요해질 경우 그때 `TechnicalPostDifficulty` 같은 명확한 정책과 함께 재도입한다.

---

### 9.7 문서-코드 동기화 상태

결정사항이 실제 코드에 반영된 상태를 추적한다. PR 머지 시 해당 항목을 업데이트한다.

| 결정사항 | 결정일 | 코드 반영 여부 | 이행 범위 | 남은 작업 |
|---|---|---|---|---|
| `ScrabPost → Bookmark` 통일 | 2026-04-16 | 부분 반영 | DTO/에러코드는 `Bookmark` 사용. Entity(`ScrabPost`), Table(`scrap_posts`), Repository 미반영. | Entity 이름 변경, Table rename 마이그레이션, Repository 이름 변경 |
| `searchWord → query` 통일 | 2026-04-16 | 미반영 | `SearchHistory.searchWord` 필드 유지 중 | 필드명 변경 + DB 컬럼 마이그레이션 |
| `EDifficultyLevel` 제거 | 2026-04-16 | 미반영 | `EDifficultyLevel.java` 파일 및 `Post` 사용처 확인 필요 | 사용처 전수 조사 후 제거 PR |
| `RecommendationSet` 표준 용어 확정 | 2026-04-16 | 미반영 | 현재 `RecommendedPost` 단건 구조 유지 중 | `RecommendationSet` 개념으로 Aggregate 리팩터링 |
| `markAsisClicked → markAsClicked` 오타 수정 | 2026-04-16 | 미반영 | `RecommendationHistory.markAsisClicked` 유지 중 | 메서드 이름 수정 + 호출부 수정 |
| `TechBlog.markCrawled()` 추가 | 2026-04-16 | 미반영 | `TechBlog` 엔티티에 `lastCrawledAt` 갱신 메서드 없음 | 도메인 메서드 추가 + `CrawlingService` 호출부 수정 |
| `User.replaceInterests()` 추가 | 2026-04-16 | 미반영 | 불변식 검증이 `InterestCommandService`에 산재 | 도메인 메서드 추가 + 서비스 레이어 리팩터링 |
| `Post.incrementViewCount()` SQL atomic UPDATE | 2026-04-16 | 미반영 | 현재 `viewCount++` JPA dirty checking 방식 | Repository에 `@Modifying @Query` 추가 |

---

## 10. 추천 표준 용어 세트

### 콘텐츠 계열

- 기술 블로그
- RSS 피드
- 피드 아이템
- 기술 게시글
- 원문 본문
- 정제 본문
- 요약
- 짧은 요약
- 게시글 키워드
- 콘텐츠 청크
- 검색 문서
- 임베딩
- 색인
- 조회수
- 출처명

### 사용자 계열

- 사용자
- 소셜 사용자
- 온보딩
- 회원 상태
- 관심 카테고리
- 관심 키워드
- 계정 프로필
- 개인화 프로필
- 프로필 벡터
- 핵심 키워드

### 활동 계열

- 읽은 게시글
- 읽은 시간
- 읽기 몰입도
- 검색 기록
- 검색어 / SearchQuery
- 북마크
- 북마크 여부

### 검색 계열

- 일반 검색
- 개인화 검색
- BM25 검색
- 시맨틱 검색
- 하이브리드 검색
- RRF
- 개인화 리랭킹
- 검색 결과
- 하이브리드 점수
- 개인화 점수
- 최종 점수

### 추천 계열

- 추천 후보군
- 추천 게시글
- 추천 목록
- 추천 이력
- 유사도 점수
- MMR 점수
- 추천 순위
- 시간 감쇠
- 읽은 게시글 제외
- 추천 재생성
- 일일 추천 생성
