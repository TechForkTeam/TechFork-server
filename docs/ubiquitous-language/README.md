# TechFork 유비쿼터스 언어

> TechFork의 표준 용어를 바운디드 컨텍스트 후보 기준으로 정리한 문서 모음입니다.  
> 도메인 전략 문서는 `../domain-strategy.md`, 전술 설계 문서는 `../tactical-design.md`, 호환용 진입 문서는 `../ubiquitous-language.md`를 사용합니다.

---

## 1. 사용 원칙

- **기준 단위는 패키지가 아니라 바운디드 컨텍스트다.** 다만 각 문서에 현재 owning package를 함께 적어 코드 탐색 경로를 명확히 한다.
- **유비쿼터스 언어 문서는 도메인 전략과 왕복한다.** 새 용어가 나오면 먼저 이 문서를 고치고, 경계가 바뀌면 `domain-strategy.md`를 함께 조정한다.
- **전략 문서에서는 `User Account`와 `Personalization Profile`을 별도 컨텍스트로 본다.** 현재 구현도 `domain/useraccount`와 `domain/personalization`으로 물리 분리되어 있다.
- 레거시 코드명(`ScrabPost`, `searchWord`, `markAsisClicked`)은 허용하되, 문서/PR/API에서는 표준 용어를 우선 사용한다.
- 각 컨텍스트 문서는 가능하면 아래 다섯 블록을 유지한다.
  1. 표준 용어
  2. 내부 glossary
  3. 혼동 금지 / legacy 메모
  4. 금지 표현 / 권장 표현
  5. 주요 근거 파일

---

## 2. 컨텍스트 문서 맵

| 바운디드 컨텍스트 | 문서 | 현재 owning package | 메모 |
|---|---|---|---|
| Source / Ingestion | [`source-ingestion.md`](./source-ingestion.md) | `src/main/java/com/techfork/domain/source` | RSS 수집, 소스 블로그, 파이프라인 시작점 |
| Post / Content | [`post-content.md`](./post-content.md) | `src/main/java/com/techfork/domain/post` | 기술 게시글 본문, 요약, 키워드, 검색 projection |
| User Account | [`user-account.md`](./user-account.md) | `src/main/java/com/techfork/domain/useraccount` | 계정, 온보딩, 관심사, 계정 프로필 |
| Personalization Profile | [`personalization-profile.md`](./personalization-profile.md) | `src/main/java/com/techfork/domain/personalization` | 개인화 프로필 생성, 벡터, 핵심 키워드, 재생성 |
| Activity | [`activity.md`](./activity.md) | `src/main/java/com/techfork/activity` | 읽기/검색/북마크 행동 기록 |
| Search | [`search.md`](./search.md) | `src/main/java/com/techfork/domain/search` | query service / read model 중심 컨텍스트 |
| Recommendation | [`recommendation.md`](./recommendation.md) | `src/main/java/com/techfork/domain/recommendation` | 추천 후보 탐색, 랭킹, 현재 추천 목록 |
| Auth / Security | [`auth-security.md`](./auth-security.md) | `src/main/java/com/techfork/domain/auth`, `src/main/java/com/techfork/global/security` | 인증 애플리케이션 서비스 + 보안 인프라 |
| Notification | [`notification.md`](./notification.md) | `src/main/java/com/techfork/domain/notification` | 알림 토큰 저장과 활성화 상태 |
| Admin / Ops | [`admin-ops.md`](./admin-ops.md) | `src/main/java/com/techfork/domain/admin` | 운영자 진입점, 수동 배치 실행, 운영 알림 연계 |

---

## 3. 교차 컨텍스트 표준 용어

### 3.1 검색어, 핵심 키워드, 게시글 키워드 구분

| 표준 용어 | 현재 코드상 표현 | 의미 | 기준 문서 |
|---|---|---|---|
| 검색어 / SearchQuery | `query` (legacy alias: `searchWord`) | 사용자가 직접 입력한 검색 문자열 | [`search.md`](./search.md), [`activity.md`](./activity.md) |
| 핵심 키워드 / KeyKeyword | `keyKeywords` | 개인화 프로필에서 추출한 대표 관심 키워드 | [`personalization-profile.md`](./personalization-profile.md) |
| 게시글 키워드 / PostKeyword | `PostKeyword.keyword` | 기술 게시글 요약 과정에서 추출된 대표 키워드 | [`post-content.md`](./post-content.md) |

### 3.2 프로필 용어 구분

| 표준 용어 | 현재 코드상 표현 | 의미 |
|---|---|---|
| 계정 프로필 | `User.nickName`, `User.description`, `User.profileImage` | 사용자에게 보이는 기본 프로필 정보 |
| 개인화 프로필 | `PersonalizationProfileDocument.profileText`, `profileVector` | 검색 리랭킹/추천용 활동 기반 프로필 |

규칙:

- 문서/PR/API에서 **“프로필” 단독 표현은 지양**한다.
- UI/설정 화면은 `계정 프로필 수정`, 추천/검색 준비 상태는 `개인화 프로필 생성/재생성`으로 쓴다.
- `PersonalizationProfileDocument`는 `domain/personalization` 패키지의 read model/projection이다.

---

## 4. 문서-코드 동기화 상태

| 결정사항 | 기준 문서 | 코드 반영 여부 | 남은 작업 |
|---|---|---|---|
| `ScrabPost → Bookmark` 통일 | [`activity.md`](./activity.md) | 반영 | legacy 문서 표현 정리, 운영 migration 적용 확인 |
| `searchWord → query` 통일 | [`activity.md`](./activity.md), [`search.md`](./search.md) | 반영 | legacy JSON alias 정리 여부 결정 |
| Activity slice DDD 계층 정리 (`Bookmark` / `ReadPost` / `SearchHistory`) | [`activity.md`](./activity.md), [`../tactical-design.md`](../tactical-design.md) | 반영 | aggregate/value object 강화, hexagonal/ID reference 후속 검토 |
| `RecommendationSet` 표준 용어 확정 | [`recommendation.md`](./recommendation.md) | 미반영 | 현재 추천 목록 aggregate 정리 |
| `markAsisClicked → markAsClicked` 오타 수정 | [`recommendation.md`](./recommendation.md) | 미반영 | 메서드명/호출부 수정 |
| `TechBlog.markCrawled()` 추가 | [`source-ingestion.md`](./source-ingestion.md) | 미반영 | 도메인 메서드 추가 + 호출부 연결 |
| `User.replaceInterests()` 추가 | [`user-account.md`](./user-account.md) | 미반영 | aggregate 불변식 검증을 도메인 메서드로 이동 |
| `Post viewCount` SQL atomic UPDATE 경로 정리 | [`post-content.md`](./post-content.md) | 반영 | `first_read_posts` 기반 dedupe ledger와 함께 유지 |
| `ReadPost` 최초 읽기 dedupe / 조회수 증가 롤백 보호 | [`activity.md`](./activity.md), [`../tactical-design.md`](../tactical-design.md) | 반영 | `first_read_at` 의미 정교화가 필요하면 별도 검토 |
| `EDifficultyLevel` 제거 | [`post-content.md`](./post-content.md) | 반영 | 필요 시 정책과 함께 재도입 검토 |

---

## 5. 다음 정리 순서

1. `domain-strategy.md`의 명칭과 glossary 문서명을 맞춘다. (`Auth` → `Auth / Security`)
2. `docs/ubiquitous-language.md`는 호환용 인덱스로 축소하고, 상세 용어는 이 디렉터리 문서에 모은다.
3. 전략 문서와 glossary는 `User Account` / `Personalization Profile`로 분리 유지한다.
4. 패키지 분리 이후에도 glossary와 전략 문서의 현재 상태 설명이 stale하지 않도록 함께 유지한다.

## 6. 내부 glossary를 채우는 기준

- **표준 용어 표**는 팀이 외부/문서/리뷰에서 써야 할 말이다.
- **내부 glossary**는 코드 안에서 실제로 보게 되는 클래스명, 필드명, 서비스명, projection 이름을 설명한다.
- **혼동 금지**에는 다른 컨텍스트 용어와 헷갈리기 쉬운 표현, 레거시 이름, 당장 rename하지 못한 이름을 적는다.
- 새 용어가 생기면 가능한 한
  - 코드명
  - 표준 용어
  - 혼동되는 인접 용어
를 함께 기록한다.
