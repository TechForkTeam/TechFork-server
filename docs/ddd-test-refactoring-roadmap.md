# DDD 전환과 테스트 개선 로드맵

> 목적: TechFork 서버를 DDD 관점으로 점진적으로 개선하면서, 아직 부족한 테스트 코드를 어떤 순서로 작성·개선할지 정리한다.  
> 관련 문서: [`docs/ubiquitous-language/README.md`](./ubiquitous-language/README.md)  
> 기준 시점: **2026-04-28 working tree**

## 1. 결론

현재 프로젝트에서는 다음 순서를 추천한다.

```text
1. DDD 목표 모델 확정
2. 테스트 갭 분석
3. 현재 동작 보호용 핵심 테스트 작성
4. 용어 리팩터링
5. 컨텍스트별 DDD 리팩터링
6. 테스트를 도메인/유스케이스 중심으로 재구성
7. 이벤트, ACL, 포트 기반으로 컨텍스트 결합 낮추기
```

중요한 점은 다음과 같다.

- **테스트를 전부 완성한 뒤 DDD로 전환하지 않는다.**
- **DDD 구조를 먼저 대규모로 바꾸지도 않는다.**
- 대신 **바꿀 슬라이스마다 현재 동작을 보호하는 테스트를 먼저 작성하고, 그 범위 안에서 DDD 리팩터링을 진행한다.**

즉, 전략은 다음과 같다.

```text
DDD 목표 지도 작성
  → 바꿀 영역 선택
  → 해당 영역의 현재 동작 보호 테스트 작성
  → 작은 리팩터링
  → 테스트 개선
  → 다음 영역으로 이동
```

2026-04-28 현재 상태를 요약하면 다음과 같다.

```text
[완료] Phase 0: DDD 기준선 문서화
[완료] Phase 1: 테스트 갭 분석 문서화
[부분 진행] Phase 2: Activity 핵심 테스트, PersonalizationProfileServiceTest,
                    PostSummary* 테스트 등 기본 안전망 반영
[부분 진행] Phase 3: Bookmark / SearchQuery 용어 정리, EDifficultyLevel 제거 반영
[다음] Activity 4.1을 Bookmark → ReadPost → SearchHistory slice로 정리
[다음] Post aggregate / embedding pipeline 테스트 보강
```

---

## 2. 왜 이 순서가 필요한가

현재 TechFork는 다음 특징을 가진다.

- 기능은 이미 여러 도메인으로 나뉘어 있다.
- 하지만 코드상 컨텍스트 간 직접 의존이 많다.
- 테스트 커버리지가 아직 충분하지 않다.
- 용어가 완전히 정리되진 않았고, **문서/코드/마이그레이션 간 드리프트**가 일부 남아 있다.
  - 예: 문서에 남은 `ScrabPost` 흔적 vs 현재 코드의 `Bookmark`
  - 예: legacy request alias `searchWord` vs canonical field `query`
  - 예: 계정 프로필과 개인화 프로필
- Search, Recommendation, Personalization Profile(`PersonalizationProfileService`) 쪽은 여러 컨텍스트와 외부 인프라가 얽혀 있어 리팩터링 위험이 크다.

따라서 안전한 전환 전략은 다음이다.

1. **방향성은 DDD 문서로 먼저 고정한다.**
2. **현재 동작을 깨뜨리지 않도록 회귀 테스트를 작성한다.**
3. **작고 의미 있는 컨텍스트부터 정리한다.**
4. **테스트 구조를 점진적으로 DDD 스타일로 개선한다.**

---

## 3. 전체 Phase 계획

### Phase 0. DDD 기준선 확정

이미 진행된 작업이다.

산출물:

- `docs/ubiquitous-language/README.md`
- `docs/ubiquitous-language/` (컨텍스트별 glossary)

포함되어야 할 내용:

- 비즈니스 도메인
- 핵심/지원/일반 하위 도메인 분류
- 유비쿼터스 언어
- 바운디드 컨텍스트
- Context Map
- 컨텍스트 간 의존 방향
- 컨텍스트 간 통신 패턴
  - Shared Kernel
  - ACL
  - Projection / Read Model
  - Query Composition
  - 동기 직접 호출
  - 이벤트 후보
- 애그리거트 루트 식별
- 도메인 이벤트 후보와 우선순위
- 용어 결정사항

현재 결정된 주요 기준:

- `Post`는 도메인 문서에서 **기술 게시글**로 설명한다.
- `ScrabPost`, `scrap_posts`, `Bookmark`는 **북마크**로 통일한다.
- `TechBlog`는 Source 컨텍스트의 RSS 소스/출처 애그리거트 루트로 유지한다.
- `Post.company`는 `TechBlog.companyName`의 비정규화 스냅샷으로 본다.
- 사용자 입력은 **검색어/SearchQuery**로 부른다.
- 프로필 대표어는 **핵심 키워드/KeyKeyword**로 부른다.
- 게시글 대표어는 **게시글 키워드/PostKeyword**로 부른다.
- 전략 문서와 glossary에서는 **`User Account`** 와 **`Personalization Profile`** 을 분리한다.
- `EDifficultyLevel`은 실제 사용처가 없어 제거 완료된 상태로 본다.

---

### Phase 1. 테스트 갭 분석

DDD 리팩터링에 들어가기 전에 테스트 현황을 먼저 파악한다.

권장 산출물:

```text
docs/test-gap-analysis.md
```

2026-04-28 현재 `docs/test-gap-analysis.md`는 이미 존재하며, 이후 로드맵 업데이트와 함께 같이 최신화하는 것을 기본 원칙으로 둔다.

분석 항목:

```text
1. 현재 존재하는 테스트
2. 컨텍스트별 누락 테스트
3. 리팩터링 전에 반드시 필요한 테스트
4. DDD 전환 후 다시 정리할 테스트
5. 외부 인프라 의존으로 인해 별도 전략이 필요한 테스트
```

컨텍스트별로 다음을 확인한다.

| 컨텍스트 | 확인할 테스트 |
|---|---|
| Activity | 읽기 기록, 조회수 증가, 북마크 추가/삭제, 검색 기록 저장 |
| Post / Content | 기술 게시글 생성, 요약 갱신, 키워드 갱신, 조회수 증가, PostDocument 생성 |
| User Account | 소셜 사용자 생성, 온보딩, 관심사 저장, 관심 키워드 검증, 계정 프로필 수정, 탈퇴 |
| Personalization Profile | 활동 데이터 기반 개인화 프로필 생성, 프로필 벡터/핵심 키워드 생성, 재생성 |
| Search | 일반 검색, 개인화 검색, fallback, RRF, 검색 결과 metadata 조립 |
| Recommendation | 후보군 생성, 읽은 게시글 제외, MMR, 기존 추천 이력화, 새 추천 저장 |
| Source / Ingestion | RSS 수집, 중복 URL 제거, RssFeedItem 변환, Post 저장 |
| Auth / Security | 토큰 발급, refresh, logout, 탈퇴 사용자 차단 |

---

### Phase 2. 현재 동작 보호용 핵심 테스트 작성

이 단계의 목표는 전체 커버리지를 올리는 것이 아니다.

목표는 다음이다.

> DDD 리팩터링 중 현재 동작이 깨지면 바로 알 수 있는 안전망을 만든다.

이 테스트들은 처음부터 완벽한 DDD 테스트일 필요는 없다.  
서비스 메서드 중심 테스트라도 괜찮다.

현재 기준 이미 반영된 대표 안전망은 다음과 같다.

- `ActivityCommandServiceTest`
- `ActivityQueryServiceTest`
- `BookmarkRepositoryTest`
- `ReadPostRepositoryTest`
- `SearchHistoryRepositoryTest`
- `ActivityControllerIntegrationTest`
- `SearchHistoryRequestTest`
- `PersonalizationProfileServiceTest`
- `PostSummaryProcessorTest`
- `PostSummaryReaderTest`
- `PostSummaryWriterTest`

### 2.1 P0 테스트 후보

리팩터링 전에 우선 작성해야 하는 테스트다.

| 우선순위 | 영역 | 테스트 목적 |
|---|---|---|
| P0 | Activity | 첫 읽기일 때만 조회수 증가 |
| P0 | Activity | 북마크 추가/중복 방지/삭제 |
| P0 | Activity | 검색 기록 저장 |
| P0 | User | 온보딩 완료 시 사용자 상태 ACTIVE 전환 |
| P0 | User | 관심 카테고리와 관심 키워드 저장 |
| P0 | User | 카테고리와 맞지 않는 관심 키워드 거부 |
| P0 | Post | RssFeedItem에서 기술 게시글 생성 |
| P0 | Post | 요약, 짧은 요약, 게시글 키워드 갱신 |
| P0 | Post | 임베딩으로 PostDocument 생성 (`PostEmbeddingProcessorTest`) |
| P0 | Post | Elasticsearch 색인 + `embeddedAt` 갱신 (`PostEmbeddingWriterTest`) |
| P0 | Personalization Profile | 활동 데이터 기반 개인화 프로필 생성 |
| P0 | Recommendation | 프로필 벡터 없으면 추천 생성하지 않음 |
| P0 | Recommendation | 기존 추천 이력화 후 새 추천 저장 |
| P0 | Search | 개인화 프로필 없으면 일반 검색 fallback |

### 2.2 테스트 작성 원칙

- 리팩터링할 영역부터 테스트를 작성한다.
- 모든 테스트를 먼저 다 작성하려고 하지 않는다.
- 외부 API, LLM, Elasticsearch는 mock/fake/adapter 테스트로 분리한다.
- 복잡한 통합 테스트보다 도메인 규칙을 보호하는 단위 테스트를 우선한다.
- 현재 동작이 이상하더라도, 의도된 변경이 아니라면 우선 현재 동작을 고정한다.

---

### Phase 3. 용어 리팩터링

DDD 전환의 첫 코드 변경은 대규모 구조 변경보다 **용어 정리**가 적합하다.

### 3.1 1순위: `ScrabPost` 계열을 북마크로 통일

2026-04-28 현재 상태:

```text
Bookmark                                  // entity/repository/service/test 용어 반영
bookmarks                                 // JPA table name 반영
bookmarkedAt                              // 필드/컬럼명 반영
V3__rename_scrap_posts_to_bookmarks.sql   // Flyway rename migration 존재
```

결정:

```text
표준 용어 = 북마크 / Bookmark
```

정리 결과:

```text
1. Activity 코드와 테스트의 주 용어는 Bookmark로 정렬되었다.
2. DB rename은 별도 검토 단계가 아니라 Flyway V3로 이미 반영되었다.
3. 남은 작업은 legacy 문서/주석/fixture 흔적 정리와 회귀 검증이다.
```

남은 확인 사항:

- 문서와 분석 리포트에 남은 `ScrabPost` 표현 정리
- 운영 환경에서 V3 migration 적용 여부 확인
- `BookmarkTest` 등 aggregate 관점 테스트 보강 여부 결정

---

### 3.2 2순위: 검색어 용어 정리

2026-04-28 현재 상태:

```text
SearchHistory.query                                        // entity canonical field
SearchHistoryRequest.query + @JsonAlias("searchWord")     // request 역호환 유지
V4__rename_search_histories_search_word_to_query.sql      // Flyway rename migration 존재
keyKeywords / PostKeyword                                 // 서로 다른 도메인 용어로 유지
```

표준 구분:

| 표준 용어 | 의미 |
|---|---|
| 검색어 / SearchQuery | 사용자가 직접 입력한 검색 문자열 |
| 핵심 키워드 / KeyKeyword | 개인화 프로필에서 추출한 대표 관심 키워드 |
| 게시글 키워드 / PostKeyword | 기술 게시글 요약 과정에서 추출된 대표 키워드 |

정리 결과:

```text
1. SearchHistory의 canonical 필드는 `query`로 정렬되었다.
2. legacy API 입력 호환성은 `@JsonAlias("searchWord")`로 유지한다.
3. DB 컬럼 rename도 별도 TODO가 아니라 Flyway V4로 이미 반영되었다.
```

남은 확인 사항:

- DTO/API 문서에서 `SearchQuery` / `query` 표현을 일관되게 유지
- `searchWord` alias를 언제 제거할지 별도 호환성 정책 결정
- Search/Personalization 문서에서 `keyKeywords`, `PostKeyword`, `query`의 경계 계속 명시

---

### 3.3 3순위: User Account / Personalization Profile 개념 분리 고정

현재 “프로필”이라는 말이 두 의미로 쓰인다.

| 표준 용어 | 코드상 표현 | 의미 |
|---|---|---|
| 계정 프로필 | `User.nickName`, `description`, `profileImage` | 사용자에게 보이는 기본 프로필 |
| 개인화 프로필 | `PersonalizationProfileDocument.profileText`, `profileVector` | 검색/추천에 쓰이는 활동 기반 LLM/임베딩 프로필 |

2026-04-28 현재 상태:

- `domain/useraccount`와 `domain/personalization` 패키지는 이미 분리되어 있다.
- `PersonalizationProfileServiceTest` 일반 테스트 lane이 존재한다.
- 다만 `InterestCommandService`가 `PersonalizationProfileService`를 직접 호출하는 결합은 남아 있다.

권장 순서:

```text
1. 문서/API 설명에서 `User Account`와 `Personalization Profile` 경계를 고정
2. 기존 `PersonalizationProfileServiceTest`를 기준 안전망으로 유지한다.
3. `PersonalizationProfileDocument`의 역할을 Personalization Profile projection으로 계속 명확히 한다.
4. `InterestCommandService -> PersonalizationProfileService` 직접 호출을 이벤트/포트 후보로 관리한다.
```

---

### 3.4 4순위: `EDifficultyLevel` 제거

2026-04-28 기준 실제 사용처가 없음을 확인했고, enum 삭제를 완료했다.

처리 결과:

```text
1. 실제 사용처 없음 확인
2. `src/main/java/com/techfork/domain/post/enums/EDifficultyLevel.java` 삭제 완료
3. 난이도 기능이 필요해질 때 정책과 함께 재도입
```

---

### Phase 4. 컨텍스트별 DDD 리팩터링

전체 프로젝트를 한 번에 바꾸지 않는다.

추천 순서:

```text
1. Activity
2. Post / Content
3. User Account
4. Personalization Profile
5. Recommendation
6. Search
7. Source / Ingestion
```

---

#### 4.1 Activity 컨텍스트

##### 왜 먼저 하는가

- 크기가 상대적으로 작다.
- 용어 부채가 어떤 slice에 남아 있는지 명확하다.
- 현재 코드 기준 Bookmark 용어 정리는 거의 끝났고, 남은 작업을 작은 단위로 쪼개기 쉽다.
- service/repository/controller 테스트 안전망이 이미 있다.
- `Bookmark`, `ReadPost`, `SearchHistory`가 독립 record aggregate처럼 동작해 PR 분할이 쉽다.

##### 목표 모델

```text
Activity
- ReadPost
- Bookmark
- SearchHistory
```

##### 현재 상태

```text
이미 존재하는 핵심 테스트
- ActivityCommandServiceTest
- ActivityQueryServiceTest
- BookmarkRepositoryTest
- ReadPostRepositoryTest
- SearchHistoryRepositoryTest
- ActivityControllerIntegrationTest
- SearchHistoryRequestTest
```

```text
이미 반영된 용어 정리
- Bookmark entity/repository/service/test 명칭
- bookmarks table / bookmarkedAt column
- SearchHistory.query + legacy searchWord alias 허용
```

##### 먼저 확인하거나 보강할 테스트

```text
ActivityCommandServiceTest
- 사용자가 기술 게시글을 처음 읽으면 조회수가 증가한다.
- 이미 읽은 기술 게시글을 다시 읽으면 조회수는 증가하지 않는다.
- 북마크를 추가할 수 있다.
- 이미 북마크한 기술 게시글은 다시 북마크할 수 없다.
- 북마크를 삭제할 수 있다.
- 검색 기록을 저장할 수 있다.
```

```text
BookmarkTest
- 같은 사용자와 기술 게시글 조합은 한 번만 북마크 가능하다.
```

```text
ReadPostTest (선택)
- 읽기 시각과 읽기 시간을 그대로 보존한다.
```

```text
SearchHistoryRequestTest / SearchHistoryTest
- `query`를 canonical field로 사용한다.
- legacy `searchWord` alias를 역호환으로 허용한다.
```

##### 리팩터링 후보

- Activity 컨텍스트 범위는 유지하되, 구현 단위는 `Bookmark → ReadPost → SearchHistory` slice로 분리
- `BookmarkTest` 추가 여부를 결정해 aggregate 불변식을 문서화
- `ReadPost`의 “첫 읽기만 조회수 증가” 규칙을 테스트/문서로 더 명확히 고정
- `SearchHistory`의 canonical `query`와 legacy alias 지원 범위를 명확히 기록
- 마지막에 Activity 전체 회귀 테스트를 다시 실행

---

#### 4.2 Post / Content 컨텍스트

##### 왜 두 번째인가

- 핵심 콘텐츠 모델이다.
- Source, Search, Recommendation이 모두 의존한다.
- 여기 정리가 되어야 다른 컨텍스트도 안정된다.

##### 목표 모델

```text
Post = 기술 게시글 aggregate root
PostKeyword = Post 내부 엔티티
PostDocument = 검색/추천용 read model
ContentChunk = 검색/추천용 projection 내부 값
```

##### 먼저 작성할 테스트

```text
PostTest
- RssFeedItem으로 기술 게시글을 생성한다.
- 요약과 짧은 요약을 갱신한다.
- 게시글 키워드를 추가한다.
- 게시글 키워드를 초기화한다.
- 조회수를 증가시킨다.
```

```text
SummaryExtractionServiceTest
- LLM 응답에서 summary, shortSummary, keywords를 파싱한다.
- 잘못된 LLM 응답을 처리한다.
```

```text
PostEmbeddingProcessorTest
- 제목/요약/본문 청크 임베딩으로 PostDocument를 생성한다.
```

##### 리팩터링 후보

- 도메인 문서에서 `Post`를 기술 게시글로 설명
- `Post.company`를 `TechBlog.companyName`의 비정규화 스냅샷으로 명시
- `PostKeyword`를 `Post` 내부 컬렉션으로 더 명확히 다룸
- `PostDocument`가 RDB 애그리거트가 아니라 projection임을 분리

---

#### 4.3 User Account 컨텍스트

##### 왜 세 번째인가

- Auth / Security, Activity, Notification이 기대는 사용자 정체성 경계를 제공한다.
- 관심사 불변식이 현재 서비스 레이어에 산재되어 있어 `User` aggregate 정리가 먼저다.
- User Account가 정리되어야 Personalization Profile 경계도 명확해진다.

##### 목표 모델

```text
User Account
- User aggregate
- UserInterestCategory
- UserInterestKeyword
- 계정 프로필
```

##### 먼저 작성할 테스트

```text
UserTest
- 소셜 사용자 생성 시 기본 상태는 PENDING이다.
- 온보딩 완료 시 ACTIVE가 된다.
- 계정 프로필을 수정할 수 있다.
- 탈퇴 시 개인정보가 null 처리되고 WITHDRAWN이 된다.
- 재활성화 시 PENDING 상태가 된다.
```

```text
InterestCommandServiceTest
- 관심 카테고리와 키워드를 저장한다.
- 카테고리와 맞지 않는 키워드는 거부한다.
- 관심사 저장 후 개인화 프로필 생성을 요청한다.
```

##### 리팩터링 후보

- `replaceInterests(List<EInterestCategory>, List<EInterestKeyword>)` 도메인 메서드 추가
- 관심사 불변식("키워드는 선택된 카테고리에 속해야 한다") 검증을 `User` aggregate 내부로 이동
- `InterestCommandService`가 리포지토리를 직접 조작하는 대신 `User.replaceInterests()`를 호출하도록 변경

---

#### 4.4 Personalization Profile 컨텍스트

##### 왜 네 번째인가

- Personalization Profile은 Recommendation, Search와 강하게 얽혀 있다.
- User Account(4.3)가 먼저 정리되어야 `UserInterestsChanged` 이벤트 흐름이 자연스럽게 정착된다.
- 현재는 `domain/personalization` 패키지로 분리돼 있지만, User Account 서비스에서 직접 호출 결합이 남아 있어 후속 분리가 필요하다.

##### 목표 모델

```text
Personalization Profile
- PersonalizationProfileDocument (개인화 검색/추천용 read model projection)
- PersonalizationProfileService (Personalization Profile 생성 서비스로 위치 재정의)
```

##### 먼저 작성할 테스트

```text
PersonalizationProfileServiceTest
- 관심사, 읽은 게시글, 북마크, 검색 기록을 모아 활동 데이터를 구성한다.
- LLM 응답에서 프로필 텍스트와 핵심 키워드를 파싱한다.
- 파싱 실패 시 fallback 정책을 따른다.
- 프로필 텍스트를 임베딩하여 개인화 프로필을 저장한다.
- 개인화 프로필 생성 후 추천 생성을 호출한다.
- 추천 생성 실패가 개인화 프로필 저장을 깨뜨리지 않는다.
```

##### 리팩터링 후보

현재 Personalization Profile 생성 서비스 의존:

```text
PersonalizationProfileService
- User 관심사
- ReadPost
- Bookmark
- SearchHistory
- PostKeyword
- LLM
- Embedding
- Recommendation
```

정리 방향:

- `PersonalizationProfileDocument`를 Personalization Profile projection으로 명확히 한다.
- `PersonalizationProfileService`를 User Account 서비스가 아닌 Personalization Profile 생성 서비스로 위치를 재정의한다.
- 관심사 변경/온보딩 완료는 장기적으로 `UserInterestsChanged`, `OnboardingCompleted` 이벤트로 분리한다.

분리 후보 (점진적으로 적용):

```text
PersonalizedProfileGenerator
UserActivityReader
LlmProfileAnalyzer
PersonalizedProfileRepository
```

단, 바로 쪼개기보다 테스트를 먼저 작성하고 점진적으로 분리한다.

---

#### 4.5 Recommendation 컨텍스트

##### 왜 다섯 번째인가

- 복잡도가 높다.
- Elasticsearch, Personalization Profile(`PersonalizationProfileDocument`), Activity, Post에 모두 의존한다.
- 테스트 없이 건드리면 위험하다.

##### 목표 모델

현재 구현은 `RecommendedPost` 단건 중심이다.

DDD 관점에서는 다음 모델이 더 자연스럽다.

```text
RecommendationSet 또는 UserRecommendations
- userId
- recommendedPosts
- generatedAt
```

현재 당장 엔티티를 바꾸지 않더라도, 도메인 개념은 “사용자별 추천 목록”으로 잡는다.

##### 먼저 작성할 테스트

```text
MmrServiceTest
- 후보가 비어 있으면 빈 결과를 반환한다.
- finalSize만큼 추천 결과를 반환한다.
- similarity와 diversity를 반영해 순위를 만든다.
```

```text
LlmRecommendationServiceTest
- 프로필 벡터가 없으면 추천을 생성하지 않는다.
- 읽은 게시글은 추천 후보에서 제외한다.
- RRF 결과를 MMR 후보로 변환한다.
- 기존 추천은 이력화한다.
- 새 추천을 저장한다.
```

##### 리팩터링 후보

- `RecommendedPost` 단건 중심에서 `RecommendationSet` 개념 도입 검토
- 읽은 게시글 제외 정책을 Activity repository 직접 호출이 아닌 정책 포트로 분리
- `PersonalizedProfileGenerated` 이벤트를 구독해 추천 생성
- 추천 이력과 현재 추천 목록의 책임 분리

---

#### 4.6 Search 컨텍스트

##### 왜 후순위인가

- 대부분 query/read model 중심이다.
- 애그리거트보다는 검색 orchestration에 가깝다.
- Elasticsearch 의존이 강해서 테스트 구성 비용이 있다.

##### 목표 모델

```text
Search는 aggregate 중심이 아니라 Query Service / Read Model 컨텍스트로 본다.
```

##### 먼저 작성할 테스트

```text
SearchServiceImplTest
- 일반 검색은 BM25 + Semantic 결과를 RRF로 결합한다.
- 개인화 프로필이 없으면 일반 검색 결과로 fallback한다.
- 개인화 프로필이 있으면 personalScore를 반영해 rerank한다.
- 검색 결과에 조회수와 북마크 여부를 붙인다.
```

##### 리팩터링 후보

- Elasticsearch 호출을 adapter로 감싸기
- `PostDocument`를 검색 read model로 명시
- `PersonalizationProfileDocument`를 Personalization Profile read model로 명시
- Activity의 북마크 여부 조회를 query composition으로 유지하되 포트 도입 검토

---

#### 4.7 Source / Ingestion 컨텍스트

##### 왜 후순위인가

- 배치, 외부 RSS, 동시성, 실패 처리 영향이 있다.
- DDD보다 파이프라인 안정성이 더 중요하다.
- 테스트 작성 난이도가 있다.

##### 목표 모델

```text
TechBlog = Source aggregate root
RssFeedItem = 외부 RSS를 내부 언어로 변환한 ACL DTO
Post 생성 = Source와 Post 사이의 Published Language 또는 이벤트 분리 후보
```

##### 먼저 작성할 테스트

```text
RssFeedReaderTest
- RSS 엔트리를 RssFeedItem으로 변환한다.
- 기존 URL은 제외한다.
- 같은 실행 내 중복 URL은 제거한다.
- RSS 실패 시 전체 배치가 죽지 않고 빈 리스트로 처리한다.
```

```text
RssToPostProcessorTest
- RssFeedItem을 기술 게시글로 변환한다.
```

##### 리팩터링 후보

- `RssFeedItem`을 Source와 Post 사이 Published Language로 명확히 정의
- RSS parsing adapter와 batch orchestration 분리
- `TechnicalPostDiscovered` 또는 `TechnicalPostSaved` 이벤트 도입 검토

---

### Phase 5. 테스트를 DDD 스타일로 재구성

초기 테스트는 서비스 메서드 중심이어도 된다.  
하지만 DDD 전환이 진행되면 테스트 구조도 다음처럼 바꾸는 것이 좋다.

```text
src/test/java/com/techfork/domain
  activity
    BookmarkTest
    ReadPostTest
    ActivityCommandServiceTest

  post
    PostTest
    PostSummaryProcessorTest
    PostEmbeddingProcessorTest

  user
    UserTest
    InterestCommandServiceTest
    PersonalizationProfileServiceTest

  recommendation
    MmrServiceTest
    LlmRecommendationServiceTest

  search
    SearchServiceImplTest

  source
    RssFeedReaderTest
    RssToPostProcessorTest
```

테스트 종류별 역할:

| 테스트 종류 | 목적 |
|---|---|
| Domain Unit Test | 애그리거트 불변식 검증 |
| Application Service Test | 유스케이스 흐름 검증 |
| Repository Test | 쿼리/영속성 검증 |
| Adapter Test | 외부 API, RSS, Elasticsearch, LLM 경계 검증 |
| Controller Test | API 계약 검증 |
| Integration Test | 주요 시나리오 end-to-end 검증 |

---

### Phase 6. 이벤트, ACL, 포트 기반 분리

#### Phase 6 진입 조건

다음 조건이 모두 충족되면 이벤트 도입을 시작한다.

```text
[ ] P0 테스트가 모두 존재하고 ./gradlew test -PexcludeIntegration 통과
[ ] PersonalizationProfileServiceTest로 Personalization Profile 생성 흐름 보호
[ ] MmrServiceTest + LlmRecommendationServiceTest로 추천 생성 핵심 흐름 보호
[ ] SearchServiceImplTest로 일반/개인화 검색 회귀 보호
[ ] User Account aggregate 책임과 Personalization Profile 생성 책임이
    서비스 수준에서 구분되어 있음 (패키지 분리는 불필요)
```

조건 미충족 상태에서 이벤트를 먼저 도입하면, 이벤트 발행/구독 경로가 테스트 안전망 없이 추가되어 리팩터링 중 회귀를 감지하기 어려워진다.

---

처음부터 이벤트 기반으로 바꾸지 않는다.

권장 순서:

```text
1. 테스트로 현재 동작 보호
2. 용어와 애그리거트 정리
3. 컨텍스트별 책임 분리
4. 이벤트/ACL/포트 도입
```

1차 이벤트 후보:

```text
UserInterestsChanged
PersonalizedProfileGenerated
TechnicalPostIndexed
```

### 이벤트별 기대 효과

| 이벤트 | 기대 효과 |
|---|---|
| `UserInterestsChanged` | 관심사 변경과 개인화 프로필 재생성을 분리 |
| `PersonalizedProfileGenerated` | Personalization Profile 생성과 추천 생성을 분리 |
| `TechnicalPostIndexed` | 검색/추천 가능한 콘텐츠 상태를 명시 |

---

## 4. 실제 실행 순서 제안

지금 기준 다음 순서를 추천한다.

```text
[완료] 1. DDD 기준선 문서 정리
[완료] 2. 테스트 갭 분석 문서 작성
[완료] 3. Bookmark / SearchQuery 용어 리팩터링 기본 반영 + EDifficultyLevel 제거
[완료] 4. Activity 핵심 테스트 / PersonalizationProfileServiceTest / PostSummary* 기본 안전망 반영
[다음] 5. Activity 4.1 후속 정리
       - Bookmark → ReadPost → SearchHistory slice로 이슈/PR 분리
[다음] 6. Post aggregate 테스트 작성
       - PostTest, PostEmbeddingProcessorTest, PostEmbeddingWriterTest
[다음] 7. User aggregate 관심사 불변식 정리
[다음] 8. Recommendation 생성 테스트 작성
       - MmrServiceTest, LlmRecommendationServiceTest
[다음] 9. SearchServiceImpl 테스트 작성
[다음] 10. Phase 6 진입 조건 충족 후 이벤트/포트 분리 시작
```

---

## 5. 작업 단위 예시

### 5.1 작업 단위 1: Activity / Bookmark 정합성 마무리

```text
목표:
- 이미 반영된 Bookmark 용어 정리를 문서/테스트/마이그레이션 관점에서 마무리한다.

선행 테스트:
- 북마크 추가
- 중복 북마크 방지
- 북마크 삭제
- 북마크 목록 조회
- Activity 전체 회귀 확인

리팩터링:
- Bookmark aggregate/entity 테스트 보강 여부 결정
- 문서와 분석 리포트에 남은 legacy `ScrabPost` 표현 정리
- 운영 환경 migration 적용 여부 확인

주의:
- 이미 추가된 V3 migration과 현재 스키마/JPA 매핑이 충돌하지 않도록 유지
```

### 5.2 작업 단위 2: Post 도메인 보호

```text
목표:
- Post를 기술 게시글 애그리거트로 명확히 한다.

선행 테스트:
- RssFeedItem에서 Post 생성
- 요약 갱신
- 키워드 갱신
- 조회수 증가

리팩터링:
- 문서/주석에서 기술 게시글 용어 사용
- PostKeyword를 Post 내부 엔티티로 명확히 관리
- EDifficultyLevel 제거 후 남은 문서/회귀 정리
```

### 5.3 작업 단위 3: Personalization Profile 경계 정리

```text
목표:
- User Account와 Personalization Profile 경계를 문서/서비스 책임 기준으로 분리한다.

선행 테스트:
- 온보딩 완료
- 관심사 저장
- 개인화 프로필 생성
- 핵심 키워드 파싱

리팩터링:
- PersonalizationProfileDocument를 Personalization Profile projection으로 명확히 함
- PersonalizationProfileService 책임 분리
- PersonalizedProfileGenerated 이벤트 도입 준비
```

### 5.4 작업 단위 4: 추천 모델 정리

```text
목표:
- 현재 추천 목록과 추천 이력을 구분한다.

선행 테스트:
- 추천 후보 생성
- 읽은 게시글 제외
- MMR 결과 생성
- 기존 추천 이력화
- 새 추천 저장

리팩터링:
- RecommendationSet 또는 UserRecommendations 개념 도입 검토
- 추천 생성 트리거를 PersonalizedProfileGenerated 이벤트로 분리
```

### 5.5 작업 단위 5: Post Embedding Pipeline 보호

```text
목표:
- 임베딩 파이프라인을 테스트로 보호하고 PostDocument 생성 흐름을 명확히 한다.

선행 테스트:
- 제목/요약/본문 청크를 각각 임베딩한다.
- PostDocument에 titleEmbedding, summaryEmbedding, contentChunks를 채운다.
- 임베딩 실패 시 예외를 전파한다.
- Elasticsearch bulk index + embeddedAt 갱신이 원자적으로 처리된다.

리팩터링:
- PostEmbeddingWriter의 embeddedAt 갱신을 도메인 메서드 markAsEmbedded()로 위임
- TechnicalPostIndexed 이벤트 도입 준비 (Phase 6)
```

### 5.6 작업 단위 6: 검색 회귀 테스트

```text
목표:
- Search 컨텍스트의 핵심 흐름을 일반 단위 테스트로 보호한다.
- evaluation suite와 별개로 빠른 회귀 감지 루프를 확보한다.

선행 테스트:
- 일반 검색은 BM25 + semantic 결과를 RRF로 결합한다.
- 개인화 프로필이 없으면 일반 검색 결과로 fallback한다.
- 개인화 프로필이 있으면 personalScore로 rerank한다.
- 검색 결과에 viewCount와 isBookmarked를 붙인다.

리팩터링:
- Elasticsearch 호출을 adapter 인터페이스로 감싸기
- PostDocument를 검색 read model로 명시
주의:
- evaluation suite(evaluation 태그)는 이 테스트와 분리 유지
```

### 5.7 작업 단위 7: Personalization Profile 경계 테스트 보강

```text
목표:
- 기존 PersonalizationProfileServiceTest를 기준 안전망으로 유지하고, 경계 분리 리팩터링을 안전하게 진행한다.

선행 테스트:
- 관심사, 읽은 게시글, 북마크, 검색 기록을 활동 데이터로 수집한다.
- LLM 응답에서 profileText와 keyKeywords를 파싱한다.
- 파싱 실패 시 fallback 정책을 따른다.
- profileText를 임베딩하여 PersonalizationProfileDocument를 저장한다.
- 개인화 프로필 생성 후 추천 생성을 호출한다.
- 추천 생성 실패가 개인화 프로필 저장을 깨뜨리지 않는다.

리팩터링:
- PersonalizationProfileDocument를 Personalization Profile projection으로 명확히 함
- PersonalizationProfileService 책임 분리 (PersonalizedProfileGenerated 이벤트 도입 준비)
```

### 5.8 작업 단위 8: 1차 이벤트 도입

```text
전제 조건:
- 5.1~5.7 작업 단위의 선행 테스트가 모두 존재하고 통과
- Phase 6 진입 조건 체크리스트 완료

목표:
- 세 이벤트를 순서대로 도입해 컨텍스트 결합도를 낮춘다.

도입 순서:
1. UserInterestsChanged
   - InterestCommandService가 PersonalizationProfileService를 직접 호출하는 대신 이벤트 발행
   - 리스너: @TransactionalEventListener(AFTER_COMMIT) + @Async

2. PersonalizedProfileGenerated
   - PersonalizationProfileService가 LlmRecommendationService를 직접 호출하는 대신 이벤트 발행
   - 리스너: 추천 생성 트리거

3. TechnicalPostIndexed
   - PostEmbeddingWriter Step 완료 후 이벤트 발행 (StepExecutionListener.afterStep)
   - 리스너: 검색/추천 콘텐츠 준비 완료 알림

검증:
- 이벤트 도입 전후 기존 테스트 전체 통과
- 각 이벤트에 대한 publisher/listener contract test 추가
```

---

## 6. 최종 요약

가장 중요한 원칙은 다음이다.

> **테스트를 DDD 전환의 선행조건으로 전부 완성하려고 하지 말고, 리팩터링할 슬라이스마다 필요한 테스트를 먼저 깔고 바로 전환한다.**

추천 순서:

```text
DDD 기준선 확정
→ 테스트 갭 분석
→ Activity 보호 테스트
→ Bookmark 용어 리팩터링
→ Post 보호 테스트
→ User/Profile 보호 테스트
→ Recommendation 보호 테스트
→ 컨텍스트별 DDD 리팩터링
→ 이벤트/ACL/포트 기반 분리
```

이 방식이 현재 TechFork처럼 테스트가 아직 부족하고, 동시에 DDD 방향으로 구조를 개선해야 하는 프로젝트에서 가장 안전하다.
