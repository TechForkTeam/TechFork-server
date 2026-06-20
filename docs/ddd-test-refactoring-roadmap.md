# DDD 전환과 테스트 개선 로드맵

> 목적: TechFork 서버를 DDD 관점으로 점진적으로 개선하면서, 아직 부족한 테스트 코드를 어떤 순서로 작성·개선할지 정리한다.  
> 관련 문서: [`docs/ubiquitous-language/README.md`](./ubiquitous-language/README.md)  
> 기준 시점: **2026-06-21, #432 Auth / Security 문서 동기화 시점**

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

2026-06-21 현재 상태를 요약하면 다음과 같다.

```text
[완료] Phase 0: DDD 기준선 문서화
[완료] Phase 1: 테스트 갭 분석 문서화
[완료] Phase 2: Activity, Post summary/embedding, User Account, Personalization Profile,
                Recommendation/Search 주요 회귀 테스트 반영
[완료] Phase 3: Bookmark / SearchQuery 용어 정리, EDifficultyLevel 제거 반영
[완료] Activity 4.1, User Account, Personalization Profile 1차 DDD 경계 정리
[완료] PersonalizedProfileGeneratedEvent로 Personalization → Recommendation 후처리 분리
[완료] Auth / Security 1차 DDD 경계 정리
        - `auth` 최상위 컨텍스트와 `auth/security` shared kernel 정리
        - User Account 조회 seam, auth cache 이벤트 seam, OAuth/OIDC 책임, DTO 경계 정리
        - OAuth 성공 redirect access token 제거와 refresh token cookie 기반 access token 재발급 계약 반영
[다음] RecommendationSet, MMR, Search/Recommendation 후속 모델 정리
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
- Search, Recommendation, Personalization Profile 쪽은 여러 컨텍스트와 외부 인프라가 얽혀 있어 리팩터링 위험이 크다.

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

- `ReadHistoryCommandServiceTest`
- `BookmarkCommandServiceTest`
- `BookmarkQueryServiceTest`
- `BookmarkRepositoryTest`
- `BookmarkTest`
- `BookmarkIntegrationTest`
- `ReadPostCommandServiceTest`
- `ReadPostQueryServiceTest`
- `ReadPostFirstReadPolicyTest`
- `ReadPostTest`
- `ReadPostRepositoryTest`
- `ReadPostIntegrationTest`
- `SearchHistoryRepositoryTest`
- `SearchHistoryIntegrationTest`
- `SearchHistoryRequestTest`
- `PersonalizationProfileServiceTest`
- `PostSummaryProcessorTest`
- `PostSummaryReaderTest`
- `PostSummaryReaderDataJpaTest`
- `PostSummaryWriterTest`
- `PostSummaryWriterDataJpaTest`
- `SummaryExtractionServiceTest`

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

2026-06-03 현재 상태:

- `useraccount`와 `personalization` 최상위 패키지는 이미 분리되어 있다.
- `PersonalizationProfileServiceTest` 일반 테스트 lane이 존재한다.
- `InterestCommandService` / `UserCommandService`가 `PersonalizationProfileService`를 직접 호출하던 결합은 `OnboardingCompletedEvent`, `UserInterestsChangedEvent` + `AFTER_COMMIT` 리스너로 분리되었다.
- Auth cache seam도 `UserWithdrawnEvent` / `OnboardingCompletedEvent`로 분리되었다. 단, 탈퇴 캐시 무효화는 보안 민감 후처리라 `BEFORE_COMMIT + AFTER_COMMIT`으로 실행한다.

권장 순서:

```text
1. 문서/API 설명에서 `User Account`와 `Personalization Profile` 경계를 계속 고정
2. `PersonalizationProfileServiceTest`와 이벤트 publisher/listener/integration 테스트를 기준 안전망으로 유지한다.
3. `PersonalizationProfileDocument`의 역할을 Personalization Profile projection/read model로 계속 명확히 한다.
4. 개인화 프로필 생성 후 추천 생성은 `PersonalizedProfileGeneratedEvent` 발행과 Recommendation 리스너 구독으로 분리되었다.
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

Phase 4는 한 번에 끝내는 단일 단계가 아니라, **두 바퀴**로 나눠서 진행한다.

```text
1차: 각 컨텍스트를 slice / 책임 / 테스트 기준으로 먼저 정리
2차: 전역 기준에 맞춰 ID reference / 값 객체 / 엔티티 경계를 정교화
```

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

#### 4.0 전술 모델 기준선 정리

컨텍스트별 구현을 바로 바꾸기 전에, **전역 전술 모델 기준**을 먼저 문서로 확정한다.

이 단계는 대규모 코드 변경 단계가 아니라 **read-only 설계 정리 단계**다.

여기서 정리할 기준:

```text
- 같은 Aggregate 내부 연관과 다른 Aggregate 간 연관을 구분한다.
- 같은 Aggregate 내부 엔티티/컬렉션은 객체 참조를 유지할 수 있다.
- 다른 Aggregate Root를 향한 연관은 장기적으로 ID reference 전환 후보로 본다.
- ManyToOne을 지금 당장 모두 금지하지는 않지만,
  cross-aggregate 참조는 2차 정리에서 검토 대상으로 명시한다.
- 값 객체(Value Object) 후보는 tactical-design 문서의 후보 표를 기준으로 우선순위를 정한다.
```

산출물:

```text
- Aggregate 경계 기준표
- same-aggregate object reference / cross-aggregate id reference 판단 기준
- VO 후보 우선순위 메모
- 컨텍스트별 2차 정리 대상 목록
```

중요 원칙:

> 컨텍스트 1차 정리(Activity, Post, User Account, ...)와
> ID reference / VO 전환을 한 PR에 섞지 않는다.

이 원칙을 지켜야 리뷰 시 “구조 정리 때문인지”, “모델 철학 변경 때문인지”를 구분할 수 있다.

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
- ReadHistoryCommandServiceTest
- BookmarkCommandServiceTest
- BookmarkQueryServiceTest
- BookmarkTest
- BookmarkRepositoryTest
- BookmarkIntegrationTest
- ReadPostCommandServiceTest
- ReadPostQueryServiceTest
- ReadPostFirstReadPolicyTest
- ReadPostTest
- ReadPostRepositoryTest
- ReadPostIntegrationTest
- SearchHistoryRepositoryTest
- SearchHistoryIntegrationTest
- SearchHistoryRequestTest
```

```text
현재 브랜치 기준 이미 반영된 slice / 용어 정리
- Bookmark / ReadPost / SearchHistory가 모두 `presentation / application / domain / infrastructure` 기준으로 정리되었다.
- Bookmark는 `application/command`, `application/query`, `application/query/lookup`, `domain`, `infrastructure`, `presentation` 구조를 사용한다.
- Bookmark / ReadPost / SearchHistory의 application 서비스는 더 이상 `UserRepository`, `PostRepository`, `PostKeywordRepository`를 직접 참조하지 않고 `UserLookupService`, `PostLookupService`, `PostKeywordLookupService`, `BookmarkLookupService`를 통해 다른 컨텍스트 조회를 수행한다.
- ReadPost는 `SaveReadPostCommand`, `GetReadPostsQuery`, `GetReadPostsResult`, `ReadPostItem`, `ReadPostConverter`로 command/query/response 경계를 분리했다.
- ReadPost의 북마크 여부 조회는 `bookmark.infrastructure.BookmarkRepository` 직접 참조 대신 `bookmark.application.query.lookup.BookmarkLookupService`를 통해 조합한다.
- ReadPost 목록 조회는 HTTP layer에서 `size`를 `1..100`으로 검증한다.
- SearchHistory는 `SearchHistoryRequest -> SaveSearchHistoryCommand -> ReadHistoryCommandService -> SearchHistoryRepository` 흐름으로 정리했다.
- `ReadPostFirstReadPolicy`로 첫 읽기 판별 규칙을 분리했다.
- bookmarks table / bookmarkedAt column 반영
- SearchHistory.query + legacy searchWord alias 허용
```

##### 먼저 확인하거나 보강할 테스트

```text
ReadHistoryCommandServiceTest
- 검색 기록을 저장할 수 있다.
```

```text
BookmarkCommandServiceTest
- 북마크를 추가할 수 있다.
- 이미 북마크한 기술 게시글은 다시 북마크할 수 없다.
- 북마크를 삭제할 수 있다.
```

```text
BookmarkQueryServiceTest
- 북마크 목록을 조회할 수 있다.
- 커서 기반 페이징이 동작한다.
- 키워드가 함께 조합된다.
```

```text
BookmarkTest / BookmarkRepositoryTest
- 같은 사용자와 기술 게시글 조합은 한 번만 북마크 가능하다.
```

```text
ReadPostCommandServiceTest / ReadPostQueryServiceTest / ReadPostFirstReadPolicyTest
- 사용자가 기술 게시글을 처음 읽으면 조회수가 증가한다.
- 이미 읽은 기술 게시글을 다시 읽으면 조회수는 증가하지 않는다.
- 읽은 게시글 목록에 키워드/북마크 여부/커서가 반영된다.
- 첫 읽기 판별 규칙이 `ReadPostFirstReadPolicy`에 고정된다.
```

```text
ReadPostTest
- 읽기 시각과 읽기 시간을 그대로 보존한다.
```

```text
SearchHistoryRequestTest / SearchHistoryRepositoryTest / SearchHistoryIntegrationTest
- `query`를 canonical field로 사용한다.
- legacy `searchWord` alias를 역호환으로 허용한다.
- 최근 검색 기록 조회와 저장 흐름을 보존한다.
```

##### 현재 브랜치 기준 후속 정리 예정

- `BookmarkTest`, `SearchHistoryTest` 같은 aggregate/entity 테스트 추가 여부를 결정해 불변식을 더 명시적으로 문서화
- `ReadPost`, `SearchHistory`를 포함한 Activity aggregate에 value object를 도입할지 2차 정리에서 검토
- hexagonal architecture(포트/어댑터) 적용은 DDD slice 정리 완료 후 Phase 6에서 검토/적용
- `ManyToOne -> ID reference` 전환은 전술 모델 2차 정리에서 별도 이슈로 검토
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
- 게시글 키워드를 새 목록으로 재구성한다.
- 빈 키워드 목록이면 기존 키워드를 모두 제거한다.
- 조회수를 증가시킨다.
```

```text
SummaryExtractionServiceTest
- LLM 응답에서 summary, shortSummary, keywords를 파싱한다.
- 잘못된 LLM 응답이면 예외를 던진다.
```

```text
PostSummaryReaderDataJpaTest
- summary가 null이거나 빈 문자열인 게시글만 읽는다.
- keyword fetch join 계약을 유지한다.
```

```text
PostSummaryWriterDataJpaTest
- H2에서 summary/shortSummary 저장을 검증한다.
- 기존 keyword 삭제 후 새 keyword 재구성을 검증한다.
```

```text
PostEmbeddingProcessorTest
- 제목/요약/본문 청크 임베딩으로 PostDocument를 생성한다.
```

##### summary pipeline 현재 우려사항

- summary 단계와 embedding 단계 상태가 아직 `summary IS NULL OR ''`, `embeddedAt IS NULL` 조합으로 암묵적으로 표현된다.
- malformed LLM JSON은 이제 fail-fast로 막히지만, 예외 타입은 `LlmException`을 재사용해 transport 실패와 response-format 실패를 같은 버킷으로 본다.
- `PostSummaryReader`는 여전히 미요약 backlog를 한 번에 메모리로 읽는 구조라, 데이터가 커지면 paging/streaming reader로 후속 전환이 필요하다.

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
- 공개 관심사 수정 유스케이스는 `UserInterestsChangedEvent`를 발행한다.
- 온보딩 내부 저장 경로(`saveUserInterests`)는 중복 후처리를 막기 위해 이벤트를 발행하지 않는다.
```

##### 리팩터링 후보

- `User.replaceInterests(List<UserInterestSelection>)` 도메인 메서드는 도입 완료
- 관심사 불변식("키워드는 선택된 카테고리에 속해야 한다") 검증은 `User` aggregate 내부로 이동 완료
- `InterestCommandService`는 리포지토리를 직접 조작하지 않고 `User.replaceInterests()`를 호출한다.

---

#### 4.4 Personalization Profile 컨텍스트

##### 왜 네 번째인가

- Personalization Profile은 Recommendation, Search와 강하게 얽혀 있다.
- User Account(4.3) 정리 후 `UserInterestsChangedEvent` / `OnboardingCompletedEvent` 기반 프로필 생성 트리거가 정착되었다.
- 현재는 `personalization` 최상위 패키지로 분리되어 있고, User Account 서비스의 직접 호출 결합은 이벤트 리스너로 분리되었다.

##### 목표 모델

```text
Personalization Profile
- PersonalizationProfileDocument (개인화 검색/추천용 Elasticsearch projection/read model)
- UserActivityCollector / PersonalizationProfileAnalyzer / PersonalizedProfileGenerator (활동 수집, LLM 분석, 저장 책임 분리)
- PersonalizationProfileService (생성 유스케이스 orchestration + 생성 완료 이벤트 발행)
```

##### 먼저 작성할 테스트

```text
PersonalizationProfileServiceTest
- 관심사, 읽은 게시글, 북마크, 검색 기록을 모아 활동 데이터를 구성한다.
- LLM 응답에서 프로필 텍스트와 핵심 키워드를 파싱한다.
- 파싱 실패 시 fallback 정책을 따른다.
- 프로필 텍스트를 임베딩하여 개인화 프로필 projection을 저장한다.
- 개인화 프로필 생성 성공 후 `PersonalizedProfileGeneratedEvent`를 발행한다.
- 프로필 생성 실패 시 이벤트를 발행하지 않는다.
```

##### 리팩터링 후보

현재 Personalization Profile 생성 서비스 의존:

```text
PersonalizationProfileService
- PersonalizedProfileGenerator

PersonalizedProfileGenerator
- UserActivityCollector
- PersonalizationProfileAnalyzer
- Embedding
- PersonalizationProfileDocumentRepository

UserActivityCollector
- User 관심사
- ReadPost
- Bookmark
- SearchHistory
- PostKeyword
```

정리 방향:

- `PersonalizationProfileDocument`는 Aggregate가 아니라 Personalization Profile projection/read model로 명확히 한다.
- `PersonalizationProfileService`는 User Account 서비스가 아닌 Personalization Profile 생성 orchestration 서비스로 위치가 재정의되었다.
- 관심사 변경/온보딩 완료는 `UserInterestsChangedEvent`, `OnboardingCompletedEvent`로 분리 완료했다.
- 개인화 프로필 생성과 추천 생성은 `PersonalizedProfileGeneratedEvent`로 분리 완료했다.

분리 후보 (점진적으로 적용):

```text
UserActivityCollector
PersonalizationProfileAnalyzer
PersonalizedProfileGenerator
PersonalizationProfileDocumentRepository
PersonalizedProfileGeneratedEvent
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
- `PersonalizedProfileGeneratedEvent`를 `AFTER_COMMIT`에서 구독해 추천 생성
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

#### 4.8 `global` 패키지 소유권 회수 원칙

##### 왜 `global`을 먼저 정리하지 않는가

- `global`은 바운디드 컨텍스트가 아니라 shared infra, 웹/API 계약, 도메인 정책 잔여물이 섞여 있는 구현 bucket이다.
- 따라서 `global`을 먼저 청소하기 시작하면 “어디로 보내야 하는가” 기준이 약해져, 잘못된 공통화나 과한 패키지 이동이 생기기 쉽다.
- DDD 관점에서는 **소유 도메인을 먼저 정리하고**, 그 과정에서 해당 도메인이 실제로 소유하는 `global` 코드를 회수하는 편이 더 안전하다.

##### 기본 원칙

```text
global-first cleanup 금지
→ owner-context-first reclaim
```

즉:

1. 먼저 바운디드 컨텍스트의 테스트/용어/aggregate 책임을 정리한다.
2. 그 다음 해당 컨텍스트가 실제로 소유하는 `global/*` 코드를 회수한다.
3. 끝까지 공통으로 남는 것만 shared/platform support로 유지한다.

##### 무엇이 진짜 공통인가

대체로 다음은 독립 컨텍스트로 보지 않고 shared/platform support로 유지 가능하다.

```text
global/common/BaseEntity, BaseTimeEntity
global/response/*
global/exception/*
global/config/* (단, 도메인 초기 데이터 제외)
global/lock/DistributedLock
```

##### 무엇이 도메인 소유인가

대표 예시는 다음과 같다.

```text
auth/security/*                     -> Auth / Security
global/elasticsearch/query/*        -> Search / Recommendation
global/util/LinearTimeDecayStrategy -> Recommendation
global/config/InitialDataConfig     -> Source / Ingestion
global/util/ContentCleaner          -> Source / Post shared content support 후보
```

##### 권장 회수 순서

현재 시점 기준 권장 회수 상태와 후속 순서는 다음과 같다.

```text
1. User Account 4.3 — 완료
   - Auth / Security shared seam(UserAuthCacheStore, UserPrincipal)과의 이벤트/조회 경계를 정리했다

2. Personalization Profile 4.4 — 완료
   - User Account와의 직접 호출 책임을 이벤트 기반으로 분리했다

3. Auth / Security — 1차 완료
   - `auth` 최상위 컨텍스트와 `auth/security` shared kernel로 정리
   - User Account aggregate 소유권은 유지하고 인증 최소 정보, principal/cache/token 정책만 소유
   - #427, #428, #429, #430, #431, #433, #436, #440, #442 기준 완료 상태를 문서/테스트 경로에 반영

4. Recommendation / Search — 다음
   - VectorQueryBuilder, LinearTimeDecayStrategy, RRF/검색 정책 support를 owning context로 회수

5. Source / Post shared content support — 후속
   - InitialDataConfig, ContentCleaner, 일부 converter/util을 owning context나 shared support로 재배치
```

##### Activity / Post 1차 정리 이후의 해석

- Activity와 Post는 1차 정리가 끝났더라도, 지금 남아 있는 `global` 의존 중 상당수는 진짜 shared support이거나 다른 컨텍스트 소유다.
- 따라서 **Activity/Post 관련 `global` 정리를 먼저 별도 작업으로 시작하지 않는다.**
- Auth / Security 1차 정리는 완료되었으므로, 이후 `Recommendation / Search`, `Source`를 정리할 때 각각의 소유 코드를 회수한다.

---

#### 4.9 전술 모델 2차 정리 (ID reference / 값 객체 / 엔티티 경계)

모든 컨텍스트를 **1차로 한 번씩 정리한 뒤**, 전역 기준에 맞춰 2차 정리를 수행한다.

여기서 다룰 대상:

```text
1. cross-aggregate 연관을 ID reference로 바꿀지 검토/적용
2. 값 객체(Value Object) 후보를 실제 코드에 도입
3. Aggregate 내부 엔티티와 값 객체 경계를 더 명확히 정리
```

권장 순서:

```text
1. Activity
2. Post / Content
3. User Account
4. Recommendation
5. Source / Ingestion
```

적용 예시:

```text
Activity
- Bookmark -> User/Post 참조를 ID reference로 바꿀지 검토

Post / Content
- PostSummary VO 도입 검토

User Account
- SocialIdentity, AccountProfile VO 도입 검토

Recommendation
- RankOrder VO 도입 검토

Source / Ingestion
- BlogUrl, RssUrl VO 도입 검토
```

주의:

```text
- 이 단계는 “모델 정교화” 단계이지, 초기 slice 분리 단계가 아니다.
- 테스트/패키지/책임 정리가 끝나기 전에 ID reference 전환을 먼저 넣지 않는다.
- Activity만 먼저 감으로 바꾸지 말고, 전역 기준을 먼저 보고 컨텍스트별로 순차 적용한다.
```

### Phase 5. 테스트를 DDD 스타일로 재구성

초기 테스트는 서비스 메서드 중심이어도 된다.  
하지만 DDD 전환이 진행되면 테스트 구조도 다음처럼 바꾸는 것이 좋다.

```text
src/test/java/com/techfork
  activity
    bookmark
      domain
        BookmarkTest
      infrastructure
        BookmarkRepositoryTest
      application
        command
          BookmarkCommandServiceTest
        query
          BookmarkQueryServiceTest
          lookup
            BookmarkLookupServiceTest
      integration
        BookmarkIntegrationTest
    readpost
      domain
        ReadPostFirstReadPolicyTest
        ReadPostTest
      infrastructure
        ReadPostRepositoryTest
      application
        command
          ReadPostCommandServiceTest
        query
          ReadPostQueryServiceTest
      integration
        ReadPostIntegrationTest
    readhistory
      presentation
        SearchHistoryRequestTest
      infrastructure
        SearchHistoryRepositoryTest
      application
        command
          ReadHistoryCommandServiceTest
      integration
        SearchHistoryIntegrationTest

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

남은 이벤트/포트 분리를 본격화하기 전 다음 조건을 확인한다.

```text
[ ] P0 테스트가 모두 존재하고 ./gradlew test -PexcludeIntegration 통과
[x] PersonalizationProfileServiceTest로 Personalization Profile 생성 흐름 보호
[x] User Account 이벤트 publisher/listener/integration 테스트로 후처리 seam 보호
[ ] MmrServiceTest + LlmRecommendationServiceTest로 추천 생성 핵심 흐름 보호
[ ] SearchServiceImplTest로 일반/개인화 검색 회귀 보호
[ ] User Account aggregate 책임과 Personalization Profile 생성 책임이
    서비스 수준에서 구분되어 있음 (패키지 분리는 불필요)
```

조건 미충족 상태에서 넓은 범위의 이벤트를 먼저 도입하면, 이벤트 발행/구독 경로가 테스트 안전망 없이 추가되어 리팩터링 중 회귀를 감지하기 어려워진다. 다만 User Account → Personalization/Auth seam은 publisher/listener/integration 테스트를 먼저 확보한 뒤 제한적으로 도입 완료했다.

---

처음부터 모든 흐름을 이벤트 기반으로 바꾸지 않는다.

권장 순서:

```text
1. 테스트로 현재 동작 보호
2. 용어와 애그리거트 정리
3. 컨텍스트별 책임 분리
4. 이벤트/ACL/포트 도입
```

1차 이벤트 상태:

```text
[완료] OnboardingCompletedEvent
[완료] UserInterestsChangedEvent
[완료] UserWithdrawnEvent
[완료] PersonalizedProfileGeneratedEvent
[후속] TechnicalPostIndexed
```

### 이벤트별 기대 효과

| 이벤트 | 기대 효과 |
|---|---|
| `OnboardingCompletedEvent` | 온보딩 완료와 개인화 프로필 생성/인증 캐시 무효화를 분리 |
| `UserInterestsChangedEvent` | 관심사 변경과 개인화 프로필 재생성을 분리 |
| `UserWithdrawnEvent` | 탈퇴 처리와 인증 캐시 무효화를 이벤트로 연결하되, 보안상 `BEFORE_COMMIT + AFTER_COMMIT`으로 처리 |
| `PersonalizedProfileGeneratedEvent` | Personalization Profile 생성과 추천 생성을 분리. 이벤트에는 프로필 벡터와 핵심 키워드 스냅샷을 포함해 ES refresh 가시성에 의존하지 않는다. |
| `TechnicalPostIndexed` | 검색/추천 가능한 콘텐츠 상태를 명시 |

---

## 4. 실제 실행 순서 제안

지금 기준 다음 순서를 추천한다.

```text
[완료] 1. DDD 기준선 문서 정리
[완료] 2. 테스트 갭 분석 문서 작성
[완료] 3. Bookmark / SearchQuery 용어 리팩터링 기본 반영 + EDifficultyLevel 제거
[완료] 4. Activity 핵심 테스트 / PersonalizationProfileServiceTest / PostSummary* 기본 안전망 반영
[완료] 5. Activity 4.1 1차 정리
       - Bookmark / ReadPost / SearchHistory slice를 `presentation / application / domain / infrastructure` 기준으로 정리
[완료] 5-1. Activity 4.1 2차 정리
       - application 서비스의 direct cross-context repository 접근을 application 간 의존으로 전환
[완료] 6. Post aggregate / summary pipeline 안전망 확장
       - PostTest, SummaryExtractionServiceTest
       - PostSummaryReaderDataJpaTest, PostSummaryWriterDataJpaTest
[완료] 6-1. Post embedding pipeline 테스트 작성
       - PostEmbeddingProcessorTest, PostEmbeddingWriterTest
[완료] 7. User Account 4.3 1차 정리
       - UserTest로 aggregate 규칙 보호
       - 관심사 불변식 aggregate 내부 이동
       - Personalization/Auth shared seam을 이벤트 기반으로 정리
[완료] 8. Personalization Profile 4.4 1차 정리
       - User Account와의 직접 호출 책임을 `OnboardingCompletedEvent` / `UserInterestsChangedEvent`로 분리
       - Auth cache 탈퇴 무효화는 `UserWithdrawnEvent` + `BEFORE_COMMIT + AFTER_COMMIT`으로 보호
[완료] 8-1. Personalization Profile 생성 책임 분리 및 최상위 패키지 승격
       - `personalization` 최상위 패키지로 이동
       - 활동 수집 / 프로필 분석 / 프로필 저장 orchestration 책임 분리
[완료] 8-2. Personalization → Recommendation 이벤트 경계 분리
       - `PersonalizedProfileGeneratedEvent` 도입
       - Recommendation 리스너가 `AFTER_COMMIT`에서 이벤트 스냅샷 기반 추천 생성
[완료] 8-3. Auth / Security 1차 경계 정리
       - #427: Auth / Security DDD 리팩터링 전 회귀 테스트 보강
       - #428: `auth` 최상위 컨텍스트와 `auth/security` shared kernel 소유 표면 정리
       - #429: Auth 서비스의 UserRepository 직접 의존 경계 정리
       - #430: User Account 이벤트 기반 auth cache 무효화 seam 안정화
       - #431: OAuth/OIDC 보안 인프라 책임 분리
       - #433: OAuth2/OIDC redirect/cookie 브라우저 보안 정책 정리
       - #436: Auth DTO를 Command/Response 경계로 분리
       - #440: OAuth 성공 redirect access token 제거 및 refresh 연동 계약 정리
       - #442: auth security 내부 store/cache/cookie 책임 명명 정리
[부분 진행] 9. Recommendation/Search 회귀 테스트 보강
       - LlmRecommendationServiceTest, SearchServiceImplTest 반영
       - MmrServiceTest 추가 필요
[후속] 10. Recommendation/Search 및 Source/Post shared support 소유권 회수
        - global-first가 아니라 owner-context-first 방식으로 진행
        - Auth / Security 1차 회수 이후 Recommendation/Search -> Source/Post shared support 순으로 회수
[다음] 11. 컨텍스트 1차 정리 후 전술 모델 2차 정리
        - aggregate / value object / ID reference / 엔티티 경계 정교화
[다음] 12. Phase 6 진입 조건 충족 후 이벤트/포트 분리 시작
        - hexagonal architecture(포트/어댑터) 적용 검토
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
- PersonalizedProfileGeneratedEvent 도입 완료
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
- 추천 생성 트리거는 이미 `PersonalizedProfileGeneratedEvent`로 분리되었으므로, RecommendationSet/이력화 모델 정리에 집중
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
- 개인화 프로필 생성 성공 후 `PersonalizedProfileGeneratedEvent`를 발행한다.
- 추천 생성 실패는 Recommendation 리스너에서 격리되어 개인화 프로필 저장 결과를 깨뜨리지 않는다.

리팩터링:
- PersonalizationProfileDocument를 Personalization Profile projection으로 명확히 함
- PersonalizationProfileService 책임 분리와 PersonalizedProfileGeneratedEvent 발행 완료
```

### 5.8 작업 단위 8: 1차 이벤트 도입

```text
전제 조건:
- 5.1~5.7 작업 단위의 선행 테스트가 모두 존재하고 통과
- User Account 이벤트와 `PersonalizedProfileGeneratedEvent`는 완료. 남은 `TechnicalPostIndexed`는 Phase 6 진입 조건 체크리스트를 계속 확인

목표:
- 완료된 User Account 이벤트 seam과 Personalization → Recommendation 이벤트 seam을 기준으로, 남은 이벤트를 순서대로 도입해 컨텍스트 결합도를 낮춘다.

도입 순서:
1. User Account 이벤트 — 완료
   - `InterestCommandService`가 `PersonalizationProfileService`를 직접 호출하는 대신 `UserInterestsChangedEvent` 발행
   - `UserCommandService.completeOnboarding`은 `OnboardingCompletedEvent` 발행
   - `UserCommandService.withdrawUser`는 `UserWithdrawnEvent` 발행
   - 개인화 프로필 리스너: `@TransactionalEventListener(AFTER_COMMIT)`
   - 인증 캐시 리스너: 온보딩은 `AFTER_COMMIT`, 탈퇴는 `BEFORE_COMMIT + AFTER_COMMIT`

2. PersonalizedProfileGeneratedEvent — 완료
   - `PersonalizationProfileService`가 프로필 projection 저장 성공 후 이벤트 발행
   - 이벤트 payload는 `userId`, `profileVector`, `keyKeywords` 스냅샷을 포함
   - `PersonalizedProfileGeneratedEventListener`가 `AFTER_COMMIT`에서 Recommendation 생성을 트리거
   - 추천 생성 실패는 프로필 생성/저장 결과와 분리

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
→ 전술 모델 2차 정리 (ID reference / VO / 엔티티 경계)
→ 이벤트/ACL/포트 기반 분리
```

이 방식이 현재 TechFork처럼 테스트가 아직 부족하고, 동시에 DDD 방향으로 구조를 개선해야 하는 프로젝트에서 가장 안전하다.
