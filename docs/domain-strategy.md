# TechFork 도메인 전략 설계

> DDD 관점에서 TechFork의 비즈니스 문제 공간을 분류하고 컨텍스트 간 관계를 정의한 문서입니다.  
> 새 팀원 온보딩, 아키텍처 논의, 컨텍스트 경계 결정 시 참조합니다.  
> 관련 문서: [유비쿼터스 언어](ubiquitous-language/README.md) | [전술 설계](tactical-design.md)

---

## 1. 서비스 도메인 요약

TechFork는 **사용자의 기술 관심사와 활동을 기반으로 한국 기업 기술 블로그의 기술 게시글을 수집·요약·검색·추천하는 개인화 기술 콘텐츠 디스커버리 서비스**로 볼 수 있다.

핵심 흐름은 다음과 같다.

```text
기술 블로그 RSS 수집
  → 신규 기술 게시글 저장
  → LLM 요약 및 키워드 추출
  → 임베딩 생성 및 Elasticsearch 색인
  → 사용자 관심사/활동 기반 개인화 프로필 생성
  → 일반 검색, 개인화 검색, 개인화 추천 제공
```


### 1.1 비즈니스 도메인과 하위 도메인 분류

DDD에서 **하위 도메인(Subdomain)** 은 비즈니스 문제 공간의 분류이고, **바운디드 컨텍스트(Bounded Context)** 는 그 문제를 코드와 모델로 나누어 다루는 해법 공간의 경계다.  
따라서 아래 분류는 패키지 구조를 그대로 옮긴 것이 아니라, TechFork가 시장에서 해결하려는 비즈니스 능력 기준으로 나눈 것이다.

#### 비즈니스 도메인

TechFork의 비즈니스 도메인은 다음으로 정의할 수 있다.

> **개발자가 한국 기업 기술 블로그의 기술 게시글을 더 잘 발견하고, 이해하고, 개인화된 방식으로 소비하도록 돕는 기술 콘텐츠 디스커버리 도메인**

이 비즈니스 도메인 안에서 핵심 경쟁력은 단순 RSS 수집이 아니라 다음에 있다.

- 기술 게시글을 검색/추천 가능한 형태로 이해하고 가공하는 능력
- 사용자 관심사와 행동을 개인화 프로필로 모델링하는 능력
- 검색어와 개인화 프로필을 함께 사용해 적절한 기술 게시글을 찾고 추천하는 능력

#### 핵심 하위 도메인 Core Subdomains

| 하위 도메인 | 관련 컨텍스트 | 왜 핵심인가 |
|---|---|---|
| 개인화 기술 콘텐츠 발견 | Search, Recommendation | TechFork의 직접적인 제품 가치다. 사용자가 원하는 기술 게시글을 얼마나 잘 찾고 추천받는지가 서비스 차별점이다. |
| 사용자 관심/행동 기반 개인화 모델링 | Personalization Profile, Activity 일부 | 관심사, 읽은 게시글, 북마크, 검색 기록을 개인화 프로필로 바꾸는 능력은 검색/추천 품질의 기반이다. |
| 기술 게시글 이해/풍부화 | Post / Content 일부 | LLM 요약, 짧은 요약, 게시글 키워드, 임베딩, 콘텐츠 청크는 검색/추천 품질을 좌우하므로 핵심에 가깝다. |

핵심 하위 도메인의 코드상 주요 후보:

- `SearchServiceImpl`
- `LlmRecommendationService`
- `MmrService`
- `PersonalizationProfileService`
- `SummaryExtractionService`
- `ContentChunkerService`
- `PostEmbeddingProcessor`
- `PostDocument`, `PersonalizationProfileDocument`

#### 지원 하위 도메인 Supporting Subdomains

| 하위 도메인 | 관련 컨텍스트 | 왜 지원인가 |
|---|---|---|
| 기술 블로그 소스 수집 | Source / Ingestion | 콘텐츠 확보에 필수지만, RSS 수집 자체는 서비스 차별점보다는 핵심 도메인을 먹여 살리는 지원 기능이다. |
| 기술 게시글 카탈로그/조회 | Post / Content 일부 | 저장된 기술 게시글의 목록, 상세, 회사별/최신/인기 조회를 제공한다. 검색/추천의 기반 데이터 관리 역할이다. |
| 사용자 활동 기록 | Activity | 개인화 품질을 위한 입력 신호를 수집한다. 행동 기록 자체보다는 이를 활용한 개인화가 핵심이다. |
| 사용자 계정/온보딩/관심사 관리 | User Account | 개인화 모델링의 입력을 받는 지원 기능이다. 관심사 taxonomy는 중요하지만 핵심 알고리즘 그 자체는 아니다. |
| 운영 배치/관리자 기능 | Admin / Ops, Source scheduler/listener | 크롤링, 요약, 임베딩, 추천 작업을 운영 가능하게 만든다. |
| 알림 토큰 관리 | Notification | 추천 알림 등 확장 기능을 지원한다. 현재 핵심 기능은 아니다. |

#### 일반 하위 도메인 Generic Subdomains

| 하위 도메인 | 관련 컨텍스트/모듈 | 왜 일반인가 |
|---|---|---|
| 인증/인가 | Auth / Security, `auth/security` | OAuth, JWT, refresh token, 권한 처리는 대부분 서비스에서 필요한 범용 문제다. |
| 외부 LLM/Embedding Provider 연동 | `global/llm` | 모델 제공자 호출, retry/rate limit 등은 범용 integration 문제다. 단, 프롬프트와 개인화/요약 정책은 핵심/지원 도메인에 속한다. |
| 검색 엔진 연동 | Elasticsearch repository/client | Elasticsearch client, index read/write 자체는 범용 인프라다. 단, 검색 랭킹 정책은 핵심 도메인이다. |
| 분산 락/캐시/배치 인프라 | Redis, Spring Batch, scheduler infra | 동시 실행 제어, 캐시, batch metadata는 범용 기술 문제다. |
| Webhook/운영 알림 전송 | `WebhookNotificationService` | 외부 메시지 전송 자체는 범용 integration이다. |
| 이미지 URL 최적화 | `CloudflareThirdPartyThumbnailOptimizer` | 썸네일 전달 최적화는 범용 지원 기술이다. |

### `global` 패키지 해석 원칙

`src/main/java/com/techfork/global`은 **독립 바운디드 컨텍스트가 아니라 shared implementation bucket**으로 해석한다.

따라서 DDD 리팩터링에서는 `global`을 먼저 정리하는 것이 아니라, **실제 소유 컨텍스트를 먼저 정리하고 그 과정에서 `global` 코드를 회수**하는 방식을 기본 원칙으로 둔다.

대표 분류는 다음과 같다.

| 현재 위치 | DDD 해석 | 기본 처리 원칙 |
|---|---|---|
| `auth/security/*` | `Auth / Security`의 앱 전역 인증/인가 shared kernel | Auth / Security 컨텍스트 내부에 두되, 여러 컨텍스트가 기대는 보안 계약임을 명시 |
| `global/llm/*` | 범용 provider adapter / AI integration | provider adapter는 shared로 유지, 프롬프트/정책은 owning context로 분리 |
| `global/elasticsearch/query/*` | Search / Recommendation 후보 탐색 정책 support | Search / Recommendation 리팩터링 시 owning context로 회수 |
| `global/util/LinearTimeDecayStrategy` | Recommendation 정책 | Recommendation 컨텍스트로 회수 |
| `global/config/InitialDataConfig` | Source 초기 데이터 bootstrap | Source / Ingestion 컨텍스트로 회수 |
| `global/common/*`, `global/response/*`, `global/exception/*`, 일부 `global/config/*`, `global/lock/*` | shared technical support | 진짜 공통이면 유지하거나 shared/platform support로 축소 |

#### 분류상 주의사항

- `User Account` 컨텍스트는 전체가 핵심은 아니다.  
  - 계정 프로필, 소셜 사용자 정보, 온보딩, 관심사 관리는 일반/지원에 가깝다.
  - 다만 Auth / Security, Activity, Notification이 기대는 사용자 정체성 경계를 제공한다.
- `Personalization Profile` 컨텍스트는 핵심 하위 도메인에 가깝다.  
  - 개인화 프로필 생성, 프로필 벡터, 핵심 키워드, 재생성 정책은 검색/추천 품질의 중심이다.
  - 현재 구현에서는 `PersonalizationProfileDocument`가 독립 aggregate보다 Elasticsearch read model/projection 성격이 강하고, 생성 책임은 `personalization` 최상위 컨텍스트가 담당한다.
- `Post / Content` 컨텍스트 전체가 핵심은 아니다.  
  - 단순 목록/상세 조회는 지원 하위 도메인이다.
  - 요약, 키워드 추출, 청크, 임베딩, 검색 문서화는 핵심 하위 도메인에 가깝다.
- `Search` 컨텍스트에서 Elasticsearch 호출 자체는 일반 하위 도메인이지만, BM25/semantic/RRF/personal reranking 조합 정책은 핵심 하위 도메인이다.
- `Recommendation` 컨텍스트는 거의 전체가 핵심 하위 도메인이다. 다만 executor, repository plumbing 같은 기술 요소는 일반/지원으로 볼 수 있다.
- `global`은 문서상 독립 컨텍스트로 다루지 않는다.
  - 실제 컨텍스트 소유권이 분명한 코드는 해당 컨텍스트로 승격한다.
  - `BaseEntity`, `BaseResponse`, `GlobalExceptionHandler`, infra config처럼 진짜 공통 지원 코드는 shared/platform support로 유지한다.


---

## 2. 바운디드 컨텍스트 후보

| 컨텍스트 | 주된 하위 도메인 유형 | 주요 책임 | 핵심 용어 |
|---|---|---|---|
| Source / Ingestion | 지원 하위 도메인 | 외부 기술 블로그 RSS를 수집하고 신규 기술 게시글로 변환 | 기술 블로그, RSS 피드, 피드 아이템, 크롤링 잡, 신규 기술 게시글 |
| Post / Content | 핵심 + 지원 하위 도메인 | 기술 게시글 본문, 요약, 키워드, 조회수, 검색 문서화 | 기술 게시글, 원문, 정제 본문, 요약, 짧은 요약, 게시글 키워드, 조회수, 콘텐츠 청크 |
| User Account | 지원 + 일반 하위 도메인 | 사용자 계정, 온보딩, 관심사, 계정 프로필 | 사용자, 소셜 사용자, 회원 상태, 관심 카테고리, 관심 키워드, 계정 프로필 |
| Personalization Profile | 핵심 하위 도메인 | 개인화 프로필 생성, 프로필 벡터, 핵심 키워드, 재생성 | 개인화 프로필, 프로필 텍스트, 프로필 벡터, 핵심 키워드, 활동 데이터 |
| Activity | 지원 하위 도메인 | 사용자 행동 데이터 기록 | 읽은 게시글, 읽은 시간, 검색 기록, 북마크 |
| Search | 핵심 하위 도메인 | 일반/개인화 검색 수행 | 검색어, BM25 검색, 시맨틱 검색, 하이브리드 검색, RRF, 개인화 리랭킹 |
| Recommendation | 핵심 하위 도메인 | 개인화 추천 생성 및 이력 관리 | 추천 후보군, 추천 게시글, 추천 이력, 유사도 점수, MMR 점수, 추천 순위 |
| Auth / Security | 일반 하위 도메인 | 인증/인가, OAuth, JWT 토큰 | 소셜 로그인, 액세스 토큰, 리프레시 토큰, 사용자 주체 |
| Notification | 지원/일반 하위 도메인 | 알림 토큰 저장 | 알림 토큰, 활성 토큰 |
| Admin / Ops | 지원 하위 도메인 | 관리자 수동 작업 및 운영 알림 | 관리자 배치 실행, 수동 크롤링, Webhook 알림 |

---

### 2.1 `User Account` / `Personalization Profile` 개념 분리

현재 문서 기준 결론은 다음과 같다.

- **전략 문서와 glossary에서는 `User Account`와 `Personalization Profile`을 별도 컨텍스트로 본다.**
- 현재 구현은 `useraccount`와 `personalization` 최상위 패키지로 물리 분리되어 있다.

의미:

1. `User` aggregate는 당분간 `User Account` 컨텍스트의 핵심 루트로 본다.
2. `PersonalizationProfileDocument`는 `Personalization Profile` 컨텍스트의 핵심 projection/read model로 본다.
3. Search/Recommendation과의 관계 해석은 `User Account`와 `Personalization Profile`을 분리해서 본다.
4. User Account → Personalization Profile/Auth shared 후처리는 1차 애플리케이션 이벤트로 분리되었다.

현재 상태 메모:

1. `UserCommandService`와 `InterestCommandService`는 사용자 상태/관심사 변경 후 이벤트만 발행한다.
2. `PersonalizationProfileEventListener`가 `OnboardingCompletedEvent`, `UserInterestsChangedEvent`를 `AFTER_COMMIT`에서 받아 개인화 프로필 생성을 요청한다.
3. `UserAuthCacheEventListener`가 `OnboardingCompletedEvent`는 `AFTER_COMMIT`, `UserWithdrawnEvent`는 `BEFORE_COMMIT + AFTER_COMMIT`으로 받아 인증 캐시를 무효화한다.
4. `PersonalizationProfileService`는 프로필 생성 orchestration과 생성 완료 이벤트 발행을 맡고, 활동 수집/LLM 분석/저장은 하위 application component로 분리되었다.
5. 추천 생성은 `PersonalizedProfileGeneratedEvent`를 통해 Recommendation 리스너가 커밋 이후 처리한다.
6. `PersonalizationProfileDocument`는 독립 write aggregate보다 검색·추천용 read model/projection에 가깝다.

향후 아래 조건이 충족되면 구현 경계를 더 세분화하는 것을 다시 검토한다.

1. Search/Recommendation이 개인화 프로필을 더 안정적인 전용 포트/Published Language로 소비해야 할 때
2. 개인화 프로필이 독립 수명주기, 재생성 정책, 실패 복구 정책을 가진 모델로 커질 때
3. Auth cache 무효화나 추천 생성 이벤트가 outbox/retry 같은 별도 복구 정책을 필요로 할 때

---

## 3. Context Map

### 3.1 개념/목표 중심 Context Map 요약

이 그림은 **현재 코드의 직접 참조를 그대로 복사한 표**가 아니라,  
**전략 문서 관점에서 어떤 컨텍스트가 어떤 역할로 협력하는지**를 먼저 보여주는 요약 맵이다.

- 구현 세부와 seam별 관계는 아래 **3.2 표**에서 본다.
- 현재 코드는 이 목표 상태보다 direct reference와 동기 직접 호출이 더 많다.
- 화살표는 **주 협력 방향 / 핵심 handoff 방향**을 뜻한다.

```text
[운영 / 진입점]
Admin / Ops
  ├─→ Source / Ingestion
  └─→ Post / Content batch

[정체성 / 계정]
Auth / Security ──→ User Account ←── Notification

[콘텐츠 확보 / 가공]
External RSS ──→ Source / Ingestion ──→ Post / Content

[행동 기록]
User Account ──→ Activity ←── Post / Content

[개인화 생성]
User Account            ┐
Activity                ├─→ Personalization Profile
Post / Content          ┘
LLM / Embedding Provider ──→ Personalization Profile

[콘텐츠 소비]
Post / Content ──→ Search ←── Personalization Profile
Activity       ──→ Search      (북마크 여부 조합)

User Account           ┐
Personalization Profile├─→ Recommendation
Post / Content         │
Activity               ┘

Post / Content ──→ Elasticsearch ←── Search / Recommendation / Personalization Profile
```

### 3.2 컨텍스트 관계와 통신 패턴

U/D 표기: **U** = Upstream(상류, 공급자), **D** = Downstream(하류, 소비자)  
관계 유형: **SK** = Shared Kernel, **CS** = Customer-Supplier, **CF** = Conformist, **OHS** = Open Host Service, **ACL** = Anti-Corruption Layer, **PL** = Published Language

읽는 순서:

1. **관계 유형(현재 As-Is)** — 지금 코드가 실제로 어떤 성격의 결합을 가지는지
2. **관계 유형(목표 To-Be)** — 앞으로 어떤 전략 관계로 정리하고 싶은지
3. **통신/통합 패턴** — 그 목표 관계를 구현할 때 바람직한 통신 방식이 무엇인지
4. **현재 구현** — 지금 코드가 실제로는 어떻게 붙어 있는지

같은 컨텍스트 pair가 여러 번 등장하면, **같은 pair 안의 다른 seam**을 뜻한다.  
즉 “한 컨텍스트 쌍 = 관계 하나”가 아니라, **“협력 seam마다 관계를 따로 본다”**는 원칙으로 읽는다.

추가 메모:

- **현재 관계 유형(As-Is)** 은 가능한 한 DDD 전략 관계로 읽되, 맞지 않는 경우는 **구현 결합 성격**을 드러내는 표현을 함께 쓴다.
- 즉, 현재 열은 “이론적으로 가장 예쁜 라벨”이 아니라 **지금 코드가 실제로 어떤 성격으로 붙어 있는지**를 우선한다.

| Pair | Seam | From (D, 소비자) | To (U, 공급자) | 관계 유형(현재 As-Is) | 관계 유형(목표 To-Be) | 통신/통합 패턴(목표) | 현재 구현 / 의존 방식 | 해석 및 개선 후보 |
|---|---|---|---|---|---|---|---|---|
| Auth / Security ↔ User Account | 사용자 식별/권한 | `Auth / Security` | `User Account` | **SK** | **SK** (Shared Kernel) | 공유 식별자/권한 kernel | `UserRepository`, `User`, `UserPrincipal` 직접 참조 | `UserId`, `Role`, `UserStatus`를 최소 공유 커널로 공유한다. User 전체 모델 공유는 줄이는 것이 목표다. |
| Auth / Security ↔ User Account | 인증 캐시 무효화 | `Auth / Security` | `User Account` | **OHS/PL 이벤트 handoff** | **OHS/PL** | `OnboardingCompletedEvent`, `UserWithdrawnEvent` | `UserAuthCacheEventListener`가 이벤트를 구독해 `UserAuthCacheService`를 호출 | 온보딩은 커밋 후 캐시 반영 지연을 허용한다. 탈퇴는 보안 민감 seam이라 `BEFORE_COMMIT`에서 실패 시 롤백하고 `AFTER_COMMIT`에서 한 번 더 무효화한다. |
| Notification ↔ User Account | 알림 토큰 소유자 식별 | `Notification` | `User Account` | **SK** (최소 shared identity) | **SK** (최소 Shared Identity) | 사용자 귀속 참조 | `NotificationToken.user` 직접 참조 | 장기적으로는 `UserId` 중심 참조로 축소할 수 있다. |
| Source / Ingestion ↔ 외부 RSS | 피드 포맷 적응 | `Source / Ingestion` | 외부 RSS | **ACL** | **ACL** (Anti-Corruption Layer) | 외부 어댑터 / 포맷 변환 | `WebClient`, Rome RSS parser | Source가 외부 RSS 형식을 내부 `RssFeedItem`으로 변환한다. 외부 스키마 변경 시 변환 로직만 수정하면 된다. |
| Source / Ingestion ↔ Post / Content | 기술 게시글 후보 전달 | `Post / Content` | `Source / Ingestion` | **CS + 동기 직접 호출** | **OHS/PL** (Open Host Service / Published Language) | 게시글 후보 Published DTO / 이벤트 handoff | 현재는 `RssFeedItem`을 `Post`로 변환하고 `PostBatchWriter`가 `posts`에 저장 | 현재 구현은 Source가 Post 생성까지 책임지는 동기 직접 호출이다. 목표 상태는 `TechnicalPostDiscovered` 같은 published language/event handoff다. |
| Source / Ingestion ↔ Post / Content | 출처 식별/표시 | `Post / Content` | `Source / Ingestion` | **직접 엔티티 참조** (약한 SK처럼 보이는 구현 결합) | **OHS/PL** | source reference / 출처 스냅샷 소비 | `Post.techBlog`가 `TechBlog` 엔티티를 직접 참조 | 지금은 direct entity reference라서 SK처럼 보이지만, 목표 상태는 `sourceId`나 출처 스냅샷 소비에 가깝다. |
| Activity ↔ User Account | 행동 주체 식별 | `Activity` | `User Account` | **직접 엔티티 참조** (의도상 최소 SK) | **SK** (최소 Shared Identity) | 사용자 귀속 참조 | `ReadPost`, `Bookmark`, `SearchHistory`가 `User`를 직접 참조 | Activity는 사용자 정체성만 알면 된다. 장기적으로는 `UserId` 중심 참조가 더 자연스럽다. |
| Activity ↔ Post / Content | 행동 대상 식별 | `Activity` | `Post / Content` | **CF** | **CF** (Conformist) | 직접 aggregate 참조 | `ReadPost`, `Bookmark`가 `Post`를 직접 참조 | Activity는 행동 대상인 기술 게시글 모델을 따른다. 장기적으로는 `PostId` 중심 참조 축소도 가능하다. |
| User Account ↔ Personalization Profile | 프로필 생성 트리거 | `Personalization Profile` | `User Account` | **OHS/PL 이벤트 handoff** | **OHS/PL** (Open Host Service / Published Language) | 애플리케이션 이벤트 / Published Language | `UserCommandService`, `InterestCommandService`가 `OnboardingCompletedEvent`, `UserInterestsChangedEvent`를 발행하고 `PersonalizationProfileEventListener`가 `AFTER_COMMIT`에서 처리 | User Account는 개인화 프로필 생성 서비스를 직접 알지 않는다. 개인화 프로필 생성 실패는 사용자 상태 변경 트랜잭션을 롤백하지 않는 후처리로 본다. |
| Personalization Profile ↔ Activity | 행동 요약 입력 | `Personalization Profile` | `Activity` | **CS** | **OHS/PL** | 조회 포트 / `UserActivitySummary` 같은 Published Language | `PersonalizationProfileService`가 Activity repository를 직접 조회 | 이 seam은 현재는 Customer-Supplier에 가깝지만, 목표 상태는 “행동 요약 언어를 소비한다”는 OHS/PL이 더 자연스럽다. |
| Personalization Profile ↔ Post / Content | 게시글 관심 신호 입력 | `Personalization Profile` | `Post / Content` | **CS** | **OHS/PL** | 게시글 메타데이터 projection / 경량 포트 | `PersonalizationProfileService`가 `PostKeyword`와 게시글 제목을 직접 읽는다 | 개인화 프로필 생성에 필요한 게시글 신호를 소비한다. 장기적으로는 경량 projection/port로 정리 가능하다. |
| Personalization Profile ↔ Recommendation | 프로필 생성 완료 handoff | `Recommendation` | `Personalization Profile` | **OHS/PL 이벤트 handoff** | **OHS/PL** | `PersonalizedProfileGeneratedEvent` | `PersonalizationProfileService`가 프로필 projection 저장 후 이벤트를 발행하고, `PersonalizedProfileGeneratedEventListener`가 `AFTER_COMMIT`에서 추천 생성을 트리거 | 이벤트 payload는 `userId`, `profileVector`, `keyKeywords` 스냅샷을 포함한다. Recommendation은 Personalization Profile을 소비하지만 소유하지 않는다. |
| Search ↔ Post / Content | 검색용 게시글 projection | `Search` | `Post / Content` | **OHS/PL** | **OHS/PL** (Open Host Service / Published Language) | Read Model / Projection 소비 | `PostDocument` 참조 | Post가 `PostDocument` projection을 제공하고 Search가 이를 후보 탐색에 소비한다. |
| Search ↔ Post / Content | 검색 결과 metadata 조합 | `Search` | `Post / Content` | **CS** | **CS** 또는 경량 조회 포트 | Query Composition | `PostRepository`로 `viewCount` 등 metadata 조회 | 후보 탐색 seam과 metadata seam을 분리해서 본다. 다음 도메인 리팩터링에서도 Search가 Post aggregate 소유권을 가져간다고 해석하지 않도록 구분한다. |
| Search ↔ Personalization Profile | 개인화 리랭킹 입력 | `Search` | `Personalization Profile` | **OHS/PL** | **OHS/PL** (Open Host Service / Published Language) | Read Model / Projection 소비 | `PersonalizationProfileDocument` 참조 | Search는 `PersonalizationProfileDocument`를 소비해 개인화 리랭킹을 수행한다. |
| Search ↔ Activity | 북마크 여부 조회 | `Search` | `Activity` | **CS** | **CS** (Customer-Supplier) | Query Composition | `BookmarkRepository`로 북마크 여부 조회 | 검색 결과 응답 조립을 위한 조회 조합이다. |
| Recommendation ↔ User Account | 추천 대상 사용자 식별 | `Recommendation` | `User Account` | **직접 엔티티 참조** (의도상 최소 SK) | **SK** (최소 Shared Identity) | 추천 대상 사용자 식별 | `User` 직접 참조 | 추천 대상 사용자의 정체성과 상태를 알아야 한다. 장기적으로는 최소 사용자 식별자/상태 공유로 축소 가능하다. |
| Recommendation ↔ Personalization Profile | 프로필 벡터/핵심 키워드 입력 | `Recommendation` | `Personalization Profile` | **OHS/PL** | **OHS/PL** (Open Host Service / Published Language) | Read Model 소비 | `PersonalizationProfileDocument` 참조 | Recommendation의 핵심 입력은 개인화 프로필 벡터와 핵심 키워드다. |
| Recommendation ↔ Activity | 읽은 게시글 제외 신호 | `Recommendation` | `Activity` | **CS** | **CS** (Customer-Supplier) | Query Composition / 정책 포트 | `ReadPostRepository`로 읽은 게시글 제외 | 추천 정책에 필요한 제외 신호를 Activity가 공급한다. 장기적으로는 exclusion set 포트로 좁힐 수 있다. |
| Recommendation ↔ Post / Content | 추천 후보 탐색 | `Recommendation` | `Post / Content` | **OHS/PL** | **OHS/PL** | 추천 후보 projection 소비 | `PostDocument` 참조 | 추천 후보 탐색은 Post projection 소비로 충분한 것이 목표 상태다. |
| Recommendation ↔ Post / Content | 추천 저장 대상 식별 | `Recommendation` | `Post / Content` | **직접 엔티티 참조** | **SK** (최소 Shared Identity) | `PostId`/참조 식별 | 현재는 JPA `Post` reference를 직접 저장 | 후보 탐색 seam과 저장 seam을 분리해서 본다. 저장 쪽은 최소 기술 게시글 식별 공유로 축소하는 것이 목표다. |
| Post / User / Personalization Profile / Recommendation / Search ↔ LLM/Embedding Provider | 모델 제공자 연동 | `Post / User / Personalization Profile / Recommendation / Search` | LLM/Embedding Provider | **ACL** | **ACL** (Anti-Corruption Layer) | Port & Adapter | `LlmClient`, `EmbeddingClient` | 외부 모델 제공자를 인터페이스로 차단한다. 제공자 변경 시 ACL만 수정하면 된다. |
| Search / Recommendation / Personalization Profile ↔ Elasticsearch | 읽기 모델 저장/조회 | `Search / Recommendation / Personalization Profile` | Elasticsearch | **읽기 모델 인프라 + ACL 성격** | **ACL** (Anti-Corruption Layer) | Projection / Search Read Model | `PostDocument`, `PersonalizationProfileDocument` | 검색/추천/개인화 프로필용 읽기 모델이다. ES 인덱스 구조 변경이 도메인 모델에 전파되지 않도록 차단한다. |
| Source / Ops ↔ Discord Webhook | 운영 알림 전송 | `Source / Ops` | Discord Webhook | **ACL** | **ACL** (Anti-Corruption Layer) | External Adapter | `WebhookNotificationService` | 운영 알림용 외부 통합이다. 도메인 핵심 모델과 분리되어야 한다. |

### 3.2 #382 phase decision notes

`Post` cross-context 정리 phase 에서는 아래 seam 을 **유지 계약** 으로 본다.

| Seam | 이번 phase 해석 | 유지 이유 | 후속 후보 |
|---|---|---|---|
| Activity → Post lookup | `PostLookupService`, `PostKeywordLookupService` 를 통한 임시 application seam | 다른 컨텍스트가 Post repository 직접 의존으로 다시 퍼지는 것을 막는다. 다만 아직 `Post` aggregate shape 를 그대로 누수한다. | lookup port / DTO 기반 published query 로 축소 |
| Search → `PostDocument` | aggregate 의존이 아니라 projection 소비 | 검색 후보 탐색과 MySQL metadata 조합을 분리해서 해석하도록 경계를 명확히 한다. | projection schema 안정화, metadata query port 분리 |
| Recommendation → `PostDocument` | aggregate 의존이 아니라 projection 소비 | 추천 후보 탐색 seam 과 추천 저장 seam 을 분리해 다른 도메인 작업 시 경계 혼동을 막는다. | projection schema 안정화, 저장용 최소 식별 공유 |
| Source → `Post.create(RssFeedItem, TechBlog)` | Source 가 정제한 현재 monolith 내부 handoff DTO 를 받는 생성 경계 | 외부 RSS raw schema를 직접 넘기지는 않지만, 여전히 Source DTO 를 Post 가 직접 참조하는 내부 결합이다. 현재 monolith 에서 허용 가능한 타협으로 본다. | Post 소유 handoff DTO, command object, 이벤트 handoff |

이번 phase 에서는 이벤트, ACL 재구성, 물리적 컨텍스트 분리보다 **현재 seam 의 의도와 한계 명시** 를 우선한다.

### 3.3 현재 통합 스타일 평가

현재 코드는 **부분적으로 이벤트 기반 seam을 도입했지만, 여전히 동기 직접 호출과 JPA 엔티티 공유가 강한 모놀리스형 구조**다.

- 이미 분리된 seam
  - User Account → Personalization Profile 프로필 생성 트리거: `OnboardingCompletedEvent`, `UserInterestsChangedEvent` + `AFTER_COMMIT` 리스너
  - User Account → Auth cache 무효화: `OnboardingCompletedEvent`, `UserWithdrawnEvent` 리스너. 단, 탈퇴는 보안 민감 seam이라 `BEFORE_COMMIT + AFTER_COMMIT` 이중 무효화
  - Personalization Profile → Recommendation 추천 생성 트리거: `PersonalizedProfileGeneratedEvent` + `AFTER_COMMIT` 리스너
- 장점
  - User Account 커맨드 서비스가 Personalization/Auth 후처리 서비스를 직접 알지 않는다.
  - 개인화 프로필 생성과 추천 생성처럼 외부 인프라 의존이 있는 후처리는 커밋 이후 실행된다.
  - 추천 생성은 이벤트 payload의 `profileVector`, `keyKeywords` 스냅샷을 사용하므로 ES refresh 직후 가시성에 의존하지 않는다.
  - 탈퇴 인증 캐시 무효화 실패는 커밋 전에 감지되어 탈퇴 트랜잭션을 롤백할 수 있다.
- 리스크
  - `Source ↔ Post`처럼 양방향 도메인 의존이 생긴다.
  - Personalization Profile 생성 흐름은 Activity/Post 신호를 조합하므로, 아직 published query/port 없이 내부 repository-access seam에 의존한다.
  - Search/Recommendation이 여러 컨텍스트의 repository/read model을 직접 조합한다.
- 개선 우선순위
  1. `TechnicalPostSaved/TechnicalPostSummarized/TechnicalPostIndexed` 이벤트로 수집·요약·색인 파이프라인을 명시화한다.
  2. Activity 이벤트를 **개인화 프로필 재생성**의 입력 스트림으로 사용할지 검토한다.
  3. Search/Recommendation의 Personalization Profile 소비를 전용 조회 포트/Published Language로 좁힌다.

---

## 4. 컨텍스트 간 핵심 흐름

```text
[Source / Ingestion]
기술 블로그(TechBlog) → RSS 피드 → 피드 아이템(RssFeedItem) → 기술 게시글(Post)

[Post / Content]
기술 게시글(Post)
  → 요약(summary) / 짧은 요약(shortSummary) / 게시글 키워드(PostKeyword)
  → 검색 문서(PostDocument) / 콘텐츠 청크(ContentChunk)
  → Elasticsearch

[User Account]
사용자(User) → 온보딩 → 관심 카테고리 / 관심 키워드 / 계정 프로필

[Personalization Profile]
사용자 + 활동 데이터 + 게시글 신호
  → 개인화 프로필(PersonalizationProfileDocument: profileText, profileVector, keyKeywords)

[Activity]
읽은 게시글(ReadPost) / 검색 기록(SearchHistory) / 북마크(Bookmark)
  → 개인화 프로필 생성 입력

[Search]
검색어(SearchQuery) → BM25 검색 + 시맨틱 검색 → RRF → 검색 결과(SearchResult)
개인화 프로필이 있으면 → 개인화 리랭킹

[Recommendation]
개인화 프로필 벡터 + 핵심 키워드
  → 추천 후보군 탐색
  → RRF
  → MMR
  → 추천 게시글(RecommendedPost)
기존 추천 게시글 → 추천 이력(RecommendationHistory)
```
