# TechFork 전술 설계

> 애그리거트 루트 식별과 도메인 이벤트 후보를 정리한 문서입니다.  
> 리팩터링, 이벤트 도입, 모델 구조 변경 작업 시 참조합니다.  
> 관련 문서: [도메인 전략 설계](domain-strategy.md) | [유비쿼터스 언어](ubiquitous-language/README.md)

---

## 1. 애그리거트 루트 식별

### 1.1 애그리거트 루트 후보 표

| 컨텍스트 | 애그리거트 루트 | 내부 엔티티 / 값 객체 / Projection | 트랜잭션 내 보장 불변식 | 현재 코드 평가 |
|---|---|---|---|---|
| Source / Ingestion | `TechBlog` | `RssFeedItem`은 DTO/ACL 결과 | `blogUrl`과 `rssUrl`은 유일해야 한다. `lastCrawledAt`은 `markCrawled(LocalDateTime)`으로만 갱신된다. 기술 블로그는 RSS 수집 대상의 기준이다. | `TechBlog`가 Source 컨텍스트의 루트로 적절하다. **`markCrawled(LocalDateTime)` 도메인 메서드 누락** — 현재 Anemic Model 위험. |
| Post / Content | `Post` | `PostKeyword`, `PostDocument`, `ContentChunk` | URL은 유일해야 한다. 요약/짧은 요약은 `updateSummaries()`로만 교체된다. 키워드는 `clearKeywords() + addKeyword()` 조합으로만 교체된다. 임베딩 완료 시각은 `markAsEmbedded(LocalDateTime)`으로만 기록된다. `incrementViewCount()`는 비원자적 연산이므로 SQL atomic UPDATE 정책 적용 필요. | `Post`가 핵심 애그리거트 루트다. `PostKeyword`는 `Post` 내부 컬렉션으로 보는 것이 자연스럽다. **`incrementViewCount()` 동시성 정책 미결정** (§1.2 참조). |
| User Account | `User` | `UserInterestCategory`, `UserInterestKeyword` | `socialType + socialId` 조합은 유일해야 한다. 상태 전이는 `PENDING → ACTIVE → WITHDRAWN → PENDING(재활성화)` 경로만 허용된다. 관심 키워드는 반드시 선택된 관심 카테고리에 속해야 한다. 관심사 교체는 `replaceInterests()`로 단일 트랜잭션 내 불변식 검증과 함께 처리된다. | `User`가 루트다. 계정/온보딩/관심사 불변식을 소유한다. **`replaceInterests()` 도메인 메서드 누락** — 불변식 검증이 서비스 레이어에 산재. |
| Personalization Profile | 명시적 쓰기 애그리거트 없음 | `PersonalizationProfileDocument`, `UserActivityData` | 같은 `userId` 기준 현재 개인화 프로필 projection은 하나만 유지된다. 프로필 텍스트, 벡터, 핵심 키워드는 함께 재생성된다. | Personalization Profile은 aggregate보다 read model / application service 중심 컨텍스트다. 현재 `PersonalizationProfileService`가 생성 책임을 가진다. |
| Activity | `ReadPost`, `Bookmark`, `SearchHistory` | 없음 | `Bookmark`는 `userId + postId` 조합이 유일해야 한다. `ReadPost`는 같은 사용자+게시글 중복 저장을 허용하되 `ReadPostFirstReadPolicy.isFirstRead()`로 최초 읽기를 구분한다. `SearchHistory`는 같은 검색어를 중복 저장한다 (동일 검색어의 반복 횟수 자체가 개인화 관심 신호가 된다). 행동 기록은 삭제되지 않고 보존된다 (북마크 제외). | 각 행동 기록이 독립 record aggregate처럼 동작한다. `Bookmark`는 `domain/activity/bookmark` slice 아래로, `ReadPost`는 `domain/activity/readpost` slice 아래로 분리되었다. `ReadPostCommandService`/`ReadPostQueryService`/`ReadPostConverter`/`ReadPostFirstReadPolicy`가 읽기 저장/조회/첫 읽기 규칙을 분담하고, SearchHistory만 현재 `ActivityCommandService`에 남아 있다. `ManyToOne -> id reference` 같은 aggregate 경계 재설계는 별도 이슈로 다루는 편이 안전하다. |
| Search | 명시적 쓰기 애그리거트 없음 | `SearchResult` DTO, `PostDocument` read model | 검색어를 기반으로 검색 결과를 계산한다. 검색 결과는 저장되는 도메인 상태가 아니라 조회 결과다. | Search는 애그리거트보다 query service/read model 중심 컨텍스트다. |
| Recommendation | **표준: `RecommendationSet`** (현재 코드: `RecommendedPost` 단건) | `RecommendedPost`, `RecommendationHistory` | 같은 `userId + rankOrder` 조합은 유일해야 한다. 새 추천 저장 전 기존 추천은 모두 `RecommendationHistory`로 이동해야 한다. `rankOrder`는 1..N 연속이어야 한다. | 현재 `RecommendedPost` 단건이 루트 역할을 하지만 `RecommendationSet` 개념으로 리팩터링 대상이다 (코드 미반영, 유비쿼터스 언어 README의 문서-코드 동기화 상태 참조). |
| Auth / Security | 독립 애그리거트 없음 | Refresh Token 저장소, `UserPrincipal` | 토큰 발급/검증/갱신을 수행한다. 사용자 자체는 User Account 컨텍스트에 속한다. | Auth / Security는 도메인 애그리거트보다 보안 애플리케이션/인프라 컨텍스트다. |
| Notification | `NotificationToken` | 없음 | 사용자별 알림 토큰과 활성 여부를 관리한다. 같은 사용자의 토큰은 하나만 활성 상태여야 한다. | 현재 행동은 약하지만 독립 루트 후보로 볼 수 있다. |
| Admin / Ops | 독립 애그리거트 없음 | Batch Job Execution, Webhook payload | 운영자가 배치를 수동 실행하거나 실패 알림을 보낸다. | 운영 유스케이스 컨텍스트이며 핵심 도메인 애그리거트는 없다. |

### 1.2 애그리거트별 상세 메모

#### `TechBlog`

- Source / Ingestion 컨텍스트의 루트.
- RSS 수집 대상의 기준이다.
- `companyName`은 기술 블로그의 표시명/출처명으로 사용된다.
- `Post.company`에 이 값을 복사해 두는 것은 조회/정렬/검색 편의를 위한 비정규화 스냅샷으로 본다.

**누락된 도메인 메서드**

- `markCrawled(LocalDateTime crawledAt)` — RSS 수집 완료 후 `lastCrawledAt`을 갱신하는 메서드가 없다. 현재 직접 필드 조작이 가능한 상태로 Anemic Model 위험이 있다. `CrawlingService`에서 호출하도록 추가 필요.

#### `Post`

- Post / Content 컨텍스트의 핵심 루트.
- 팀 용어로는 **기술 게시글**로 설명한다.
- `PostKeyword`는 독립 루트라기보다 `Post`의 키워드 컬렉션이다.
- `PostDocument`, `ContentChunk`는 Elasticsearch 검색/추천용 projection이지 RDB 애그리거트 루트가 아니다.

**`incrementViewCount()` 동시성 정책**

현재 `viewCount++` 연산은 JPA dirty checking 기반 비원자적 업데이트다. 동시 요청 시 Lost Update가 발생한다.

- **결정**: SQL atomic UPDATE 방식 적용
  ```java
  @Modifying
  @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
  void incrementViewCount(@Param("id") Long id);
  ```
- `@Version` 낙관적 락은 재시도 비용이 발생하고 조회수 같은 통계성 필드에는 부적합하므로 채택하지 않는다.
- 현재 `isFirstRead` 체크로 사용자 중복 카운트는 방지하고 있으나, 다수 사용자 동시 접근 시 레이스 컨디션은 여전히 존재한다.

**누락된 도메인 메서드**

- `markAsEmbedded(LocalDateTime embeddedAt)` — 임베딩 완료 시각을 기록하는 명시적 메서드가 없다. 현재 `PostEmbeddingWriter`에서 직접 setter 방식으로 처리될 위험이 있다.

#### `User` / User Account

- User Account 컨텍스트의 핵심 루트.
- `UserInterestCategory`, `UserInterestKeyword`를 소유한다.
- 관심 키워드는 반드시 선택한 관심 카테고리에 속해야 한다.
- 문서/리뷰/PR에서는 `프로필` 단독 표현을 지양하고, **계정 프로필**과 **개인화 프로필**을 구분한다.

**상태 전이 규칙**

```
createSocialUser() → PENDING
     ↓ completeOnboarding (updateUser)
   ACTIVE
     ↓ withdraw()
 WITHDRAWN
     ↓ reactivate()
   PENDING
```

- `WITHDRAWN` 상태 사용자는 개인정보가 null 처리된다 (`nickName`, `email`, `profileImage`, `description`).
- `ACTIVE` 상태가 아닌 사용자는 개인화 프로필 생성, 추천, 검색 결과 조회 시 차단된다.

**누락된 도메인 메서드**

- `replaceInterests(List<EInterestCategory> categories, List<EInterestKeyword> keywords)` — 관심사 교체 시 "키워드는 선택된 카테고리에 속해야 한다"는 불변식을 Aggregate 내에서 검증하고 단일 트랜잭션으로 처리해야 한다. 현재 `InterestCommandService`가 `UserInterestCategory`/`UserInterestKeyword` 리포지토리를 직접 조작하며 불변식 검증이 서비스 레이어에 산재되어 있다.

#### `PersonalizationProfileDocument` / Personalization Profile

- Personalization Profile 컨텍스트의 핵심 projection/read model.
- 활동 데이터, 관심사, 게시글 신호를 바탕으로 검색/추천용 개인화 프로필을 생성한다.
- 현재는 독립 write aggregate보다 `PersonalizationProfileService`가 생성하는 파생 모델에 가깝다.
- 장기적으로는 `OnboardingCompleted`, `UserInterestsChanged`, `PersonalizedProfileGenerated` 이벤트 기반으로 분리할 후보다.

#### `ReadPost`, `Bookmark`, `SearchHistory`

- Activity 컨텍스트의 행동 기록 루트 후보.
- `ReadPost`는 읽기 행위, `SearchHistory`는 검색어 기록, `Bookmark`는 저장 행위를 나타낸다.
- 현재 코드명 `ScrabPost`는 표준 용어 `Bookmark`로 통일한다.

#### `RecommendationSet` / `RecommendationHistory`

표준 용어는 **`RecommendationSet`** 으로 확정한다. 현재 코드의 `RecommendedPost` 단건 구조는 리팩터링 대상이다.

- `RecommendationSet`은 특정 사용자의 현재 추천 게시글 목록 전체를 나타내는 개념적 루트다.
- 현재 구현에서는 `RecommendedPost` 단건이 저장 단위 역할을 하지만, 비즈니스 개념은 "사용자별 현재 추천 목록"에 가깝다.
- 추천 재생성 시 기존 `RecommendedPost` 목록이 `RecommendationHistory`로 이동하는 패턴이 `RecommendationSet` 교체 개념을 암묵적으로 구현하고 있다.

**`RecommendationSet` 불변식**

- 같은 `userId + rankOrder` 조합은 유일해야 한다.
- `rankOrder`는 1..N 연속이어야 한다 (중간 빈 값 없음).
- 새 추천 목록 저장 전 기존 추천은 모두 `RecommendationHistory`로 이동해야 한다 (원자적 교체).
- 한 사용자의 현재 추천 목록(`RecommendedPost`) 크기는 고정된 상한(현재 20개)을 초과하지 않는다.

**`RecommendationHistory.markAsClicked()` 오타 수정 필요**

현재 코드명 `markAsisClicked` → `markAsClicked`로 변경 필요. (`asis`는 오타)

---

### 1.3 값 객체(Value Object) 후보

현재 코드는 대부분 primitive 필드로 처리하고 있다. 아래는 향후 리팩터링 시 VO로 추출하면 불변식 검증과 의미 명확성이 높아지는 후보 목록이다. 지금 당장 만들 필요는 없으며, 각 Aggregate 리팩터링 시점에 판단한다.

| Aggregate | 필드 | VO 후보명 | 추출 이유 |
|---|---|---|---|
| `TechBlog` | `blogUrl`, `rssUrl` | `BlogUrl`, `RssUrl` | URL 형식 검증, 유일성 보장을 Aggregate 생성 시점에 VO 생성자로 위임할 수 있다. |
| `User` | `socialType + socialId` 조합 | `SocialIdentity` | 두 필드가 항상 함께 쓰이고, 조합 유일성이 불변식이다. VO로 묶으면 `equals`/유일성 검증이 명확해진다. |
| `User` | `nickName`, `email`, `profileImage`, `description` | `AccountProfile` | 계정 프로필 수정(`updateProfile()`)과 탈퇴 시 null 처리(`withdraw()`)가 같은 필드 묶음에 적용된다. |
| `RecommendedPost` | `rankOrder` | `RankOrder` | 1..N 연속 불변식을 생성 시점에 검증하는 VO로 표현할 수 있다. |
| `PersonalizationProfileDocument` | `profileVector` (float[]) | `EmbeddingVector` | 벡터 차원 수 검증, 유사도 계산 메서드를 VO에 위치시킬 수 있다. |
| `Post` | `summary`, `shortSummary` | `PostSummary` | `updateSummaries()` 단일 메서드로만 교체되는 두 필드를 VO로 묶으면 불변식이 명확해진다. |

> 주의: VO 추출은 "불변식 검증 책임이 어디에 있어야 하는가"를 기준으로 판단한다. 단순히 필드를 묶기 위한 추출은 오히려 복잡도를 높인다.

---

## 2. 도메인 이벤트 후보와 우선순위

현재 코드에는 명시적인 Domain Event 객체가 없다. 아래 표는 향후 이벤트화할 때의 우선순위다.

우선순위 기준:

- **P0**: 핵심 파이프라인 결합을 줄이거나 기능 정합성에 직접 영향이 큰 이벤트
- **P1**: 개인화 품질, 분석, 사용자 경험 개선에 중요한 이벤트
- **P2**: 운영/감사/확장성 측면에서 유용한 이벤트
- **P3**: 현재 기능에는 낮은 우선순위이지만 장기적으로 고려할 수 있는 이벤트

| 우선순위 | 이벤트 후보 | 영문 클래스명 | 발생 지점 | 주요 구독자 / 후속 처리 | 이유 |
|---|---|---|---|---|---|
| P0 | 신규 기술 게시글이 저장됨 | `TechnicalPostSaved` | `PostBatchWriter` | Summary/Embedding pipeline | 수집과 요약/색인을 이벤트로 분리하면 크롤링 파이프라인 결합도를 줄일 수 있다. |
| P0 | 기술 게시글 요약이 생성됨 | `TechnicalPostSummarized` | `PostSummaryProcessor`, `PostSummaryWriter` | Embedding/Indexing pipeline | 요약 완료 후 임베딩 생성이 가능해지는 핵심 상태 전이다. |
| P0 | 기술 게시글이 색인됨 | `TechnicalPostIndexed` | `PostEmbeddingWriter` | Search, Recommendation | 검색/추천 가능한 콘텐츠가 되었음을 나타내는 핵심 이벤트다. |
| P0 | 온보딩이 완료됨 | `OnboardingCompleted` | `UserCommandService.completeOnboarding` | 개인화 프로필 생성 | 사용자 상태가 ACTIVE가 되고 관심사가 저장되어 개인화 프로필 생성이 가능해진다. |
| P0 | 사용자 관심사가 변경됨 | `UserInterestsChanged` | `InterestCommandService.updateUserInterests` | 개인화 프로필 재생성, 추천 재생성 | 현재도 관심사 변경 후 개인화 프로필 생성이 호출된다. 이벤트로 분리하기 좋은 지점이다. |
| P0 | 개인화 프로필이 생성됨 | `PersonalizedProfileGenerated` | `PersonalizationProfileService.generatePersonalizationProfileSync` | 추천 생성, 개인화 검색 준비 완료 | 현재 `PersonalizationProfileService`가 추천 생성을 직접 호출한다. 이벤트 분리 우선순위가 높다. |
| P0 | 추천이 생성됨 | `RecommendationsGenerated` | `LlmRecommendationService.generateRecommendationsForUser` | Notification, Analytics | 사용자에게 보여줄 현재 추천 목록이 바뀌는 핵심 이벤트다. |
| P1 | 기술 게시글을 읽음 | `TechnicalPostRead` | `ReadPostCommandService.saveReadPost` | 개인화 프로필 갱신, 추천 정책 | 읽기 행동은 개인화 프로필과 읽은 게시글 제외 정책의 핵심 입력이다. |
| P1 | 기술 게시글을 처음 읽음 | `TechnicalPostFirstRead` | `ReadPostFirstReadPolicy.isFirstRead` + `Post.incrementViewCount` | 인기순 정렬, 분석 | 조회수 증가와 인기순 정렬에 직접 연결된다. |
| P1 | 기술 게시글을 북마크함 | `TechnicalPostBookmarked` | `BookmarkCommandService.addBookmark` | 개인화 프로필 갱신, 추천 튜닝 | 강한 선호 신호로 개인화 품질에 중요하다. |
| P1 | 북마크가 해제됨 | `BookmarkRemoved` | `BookmarkCommandService.deleteBookmark` | 개인화 프로필 갱신, 추천 튜닝 | 선호 신호 제거로 볼 수 있다. |
| P1 | 검색어가 기록됨 | `SearchQueryRecorded` | `saveSearchHistory` | 개인화 프로필 갱신, 검색 분석 | 검색 의도는 개인화 프로필의 주요 입력이다. |
| P1 | 추천 게시글이 클릭됨 | `RecommendationClicked` | `RecommendationHistory.markAsisClicked` 후보 | 추천 분석 | 추천 품질 평가와 모델 개선에 중요하지만 현재 호출 흐름은 약하다. |
| P2 | RSS 크롤링이 요청됨 | `RssCrawlingRequested` | `CrawlingService.executeCrawling` | 운영 모니터링 | 운영 추적과 중복 실행 분석에 유용하다. |
| P2 | RSS 피드 수집에 실패함 | `RssFeedFetchFailed` | `RssFeedReader.fetchFeedSafely`, listener | 운영 알림, 운영 모니터링 | 운영 알림/장애 대응 이벤트다. |
| P2 | 추천이 이력화됨 | `RecommendationsArchived` | 기존 `RecommendedPost` → `RecommendationHistory` | 추천 분석 | 추천 품질 분석과 장기 히스토리 분석에 유용하다. |
| P2 | 사용자가 탈퇴함 | `UserWithdrawn` | `User.withdraw` | 인증 캐시 제거, 개인정보 정리, 알림 토큰 정리 | 개인정보 정리, 토큰 무효화, 알림 토큰 비활성화와 연결된다. |
| P3 | 기술 블로그가 등록됨 | `TechBlogRegistered` | `TechBlog.create` | 크롤링 스케줄 갱신 | 현재 초기 데이터 중심이라 낮은 우선순위지만 관리 기능이 생기면 중요해진다. |
| P3 | 알림 토큰이 등록됨 | `NotificationTokenRegistered` | `NotificationToken` | 알림 발송 | 추천 알림 등 푸시 기능이 본격화되면 중요해진다. |
| P3 | 알림 토큰이 비활성화됨 | `NotificationTokenDeactivated` | `NotificationToken` | 알림 발송 중단 | 사용자 탈퇴/토큰 교체 시 연결된다. |

### 이벤트 도입 1차 추천 범위

1차로 도입한다면 다음 3개가 가장 효과가 크다.

1. `UserInterestsChanged`
2. `PersonalizedProfileGenerated`
3. `TechnicalPostIndexed`

이 세 이벤트는 각각 **개인화 프로필 재생성**, **추천 재생성**, **검색/추천 콘텐츠 준비 완료**를 분리할 수 있어 컨텍스트 결합도 감소 효과가 크다.

---

## 3. Cross-Aggregate 참조 정책

### 3.1 결정

현재 코드는 JPA `@ManyToOne`으로 다른 Aggregate의 Entity를 직접 참조하고 있다 (예: `Post → TechBlog`, `Bookmark → User, Post`, `RecommendedPost → User, Post`).

**결정: 현재 모놀리스 구조에서는 JPA 직접 참조를 유지한다. 단, 서비스 레이어에서 다른 Aggregate를 변경하는 코드는 금지한다.**

이유:
- 현재 단일 JVM·단일 DB 모놀리스에서 ID 참조로 전환하면 N+1 문제와 코드 복잡도가 늘어난다.
- 물리적 분리(MSA, 별도 DB)가 결정되기 전까지 ID 참조 전환은 비용 대비 효과가 낮다.

### 3.2 허용 규칙

| 허용 | 금지 |
|---|---|
| 다른 Aggregate의 Root를 `@ManyToOne`으로 참조하여 읽기 | 서비스 레이어에서 다른 Aggregate의 상태를 직접 변경 |
| 조회/검색 결과 조합 시 cross-aggregate 읽기 | 한 트랜잭션에서 두 Aggregate Root를 동시에 변경 |
| Projection(read model)에서 여러 컨텍스트 데이터 조합 | Aggregate 내부 엔티티(자식)를 다른 Aggregate에서 직접 참조 |

### 3.3 예외 케이스

- `PersonalizationProfileService`는 현재 Personalization Profile 쪽 생성 책임을 맡는 Application Service다. `User`, `Activity`, `Recommendation` 등 여러 컨텍스트를 조합하지만, 각 Aggregate의 상태 변경은 해당 Aggregate의 도메인 메서드를 통해서만 수행해야 한다.
- 향후 컨텍스트 분리가 필요해지면 그 시점에 직접 참조를 ID 참조로 전환하고 ACL 또는 Anti-Corruption Layer를 도입한다.

---

## 4. Domain Event 발행/구독 규약

### 4.1 이벤트 네이밍 컨벤션

| 규칙 | 예시 |
|---|---|
| PascalCase + 과거시제 동사구 | `UserInterestsChanged`, `TechnicalPostIndexed` |
| 주어(Aggregate) + 동사(상태 변화) | `PersonalizedProfileGenerated`, `RecommendationsCreated` |
| 한글 설명은 이벤트 표 주석으로만 사용, 코드명은 영문 | 표 §2의 한글 이름은 설명용, 실제 클래스명은 영문 |

### 4.2 발행/구독 기술 규약 (Spring)

```java
// 발행: 트랜잭션 커밋 후 발행 보장
// ApplicationEventPublisher.publishEvent()는 같은 트랜잭션 내 동기 호출
// → 반드시 @TransactionalEventListener(phase = AFTER_COMMIT) 리스너와 함께 사용

// 발행 지점 예시 (서비스 레이어)
applicationEventPublisher.publishEvent(new PersonalizedProfileGeneratedEvent(userId));

// 구독 지점 예시 (비동기 처리)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async("domainEventExecutor")
public void handleProfileGenerated(PersonalizedProfileGeneratedEvent event) { ... }
```

핵심 규칙:
- **모든 도메인 이벤트 리스너는 `@TransactionalEventListener(phase = AFTER_COMMIT)`을 사용한다.**  
  이유: 트랜잭션 롤백 시 이벤트가 발행되지 않도록 보장해야 한다.
- **비동기 리스너에는 반드시 `@Async`와 전용 `Executor`를 지정한다.**  
  이유: `@Async` 없이 `AFTER_COMMIT` 단독 사용 시 같은 스레드에서 동기 실행된다.
- **이벤트 발행은 Aggregate 메서드 내부가 아닌 Application Service에서 수행한다.**  
  이유: Spring의 `ApplicationEventPublisher`는 인프라 의존성이므로 도메인 레이어에 주입하지 않는다.

### 4.3 Spring Batch 내 이벤트 발행 정책

Spring Batch는 chunk 단위 트랜잭션으로 동작하므로 `@TransactionalEventListener(AFTER_COMMIT)`이 chunk commit마다 실행된다.

**결정: 배치 내 이벤트는 chunk 단위가 아닌 Step 완료 후 발행한다.**

```java
// StepExecutionListener.afterStep()에서 이벤트 발행
// → chunk 처리 중 부분 실패가 있어도 최종 완료된 범위로만 이벤트 발행 가능
@Override
public ExitStatus afterStep(StepExecution stepExecution) {
    if (stepExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
        applicationEventPublisher.publishEvent(new TechnicalPostIndexedEvent(...));
    }
    return stepExecution.getExitStatus();
}
```

이유:
- chunk 단위 이벤트 발행은 부분 실패 시 후속 처리가 중복 실행될 수 있다.
- Step 완료 후 단건 이벤트 발행이 멱등성 보장에 유리하다.

**skip count가 있는 경우 주의사항**

`ExitStatus.COMPLETED`여도 skip policy에 의해 일부 item이 건너뛰어진 경우 `stepExecution.getSkipCount() > 0`일 수 있다.  
skip이 허용 범위 내라면 COMPLETED로 끝나는 배치 특성상, 이 경우에도 이벤트를 발행할지 여부를 결정해야 한다.

**현재 정책**: skip count 여부와 무관하게 `COMPLETED` 상태이면 이벤트를 발행한다.  
이유: 색인 누락된 item은 `summaryAndEmbeddingJob` 재색인 잡이 복구하며, 이벤트 발행 억제보다 후속 재처리가 더 안전하다.  
skip count 임계값 초과 시 Step을 `FAILED`로 끝내는 skip limit 설정으로 이상 상황을 조기에 차단하는 것을 권장한다.

### 4.4 Elasticsearch Projection 일관성 정책

`PostDocument`, `PersonalizationProfileDocument`는 RDB Aggregate의 read model projection이다.

**현재 정책: 동기 즉시 갱신 (RDB 저장과 같은 트랜잭션 외부에서 ES 저장 직접 호출)**

| 시나리오 | 처리 방법 |
|---|---|
| 정상: RDB 저장 성공, ES 저장 성공 | 완료 |
| 이상: RDB 저장 성공, ES 저장 실패 | 배치 재색인 잡(`summaryAndEmbeddingJob`)으로 복구 |
| 이상: RDB 롤백 후 ES 저장 시도 | `@TransactionalEventListener(AFTER_COMMIT)` 패턴으로 방지 |

향후 ES 저장 실패율이 높아지면 `TechnicalPostIndexed` 이벤트 기반 비동기 색인 + 재시도 큐 도입을 검토한다.
