# Recommendation

> 개인화 추천 후보 탐색, 랭킹, 현재 추천 목록/이력을 다루는 핵심 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/domain/recommendation`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 추천 대상 사용자 | `User user` | 추천을 생성할 활성 사용자 |
| 추천 후보군 | `MmrCandidate` | 사용자 프로필 벡터로 검색한 기술 게시글 후보 |
| 추천 게시글 | `RecommendedPost` | 현재 사용자에게 노출할 추천 결과 |
| 추천 목록 | `RecommendationSet` (표준), 현재 코드: `List<RecommendedPost>` | 사용자별 현재 추천 게시글 묶음 |
| 추천 이력 | `RecommendationHistory` | 과거 추천 결과를 보관한 기록 |
| 유사도 점수 | `similarityScore` | 사용자 프로필과 게시글 후보 간 관련성 점수 |
| MMR 점수 | `mmrScore` | 관련성과 다양성을 함께 고려한 추천 선택 점수 |
| 추천 순위 | `rankOrder`, `rank` | 사용자에게 보여줄 추천 순서 |
| 추천 생성 시각 | `recommendedAt` | 추천이 생성된 시각 |
| 추천 클릭 | `isClicked`, `clickedAt`, `markAsClicked` | 추천 이력에서 클릭 여부를 기록하는 개념 |
| 읽은 게시글 제외 | `createExcludeFilter(readPostIds)` | 이미 읽은 기술 게시글을 추천 후보에서 제외하는 정책 |
| 시간 감쇠 | `TimeDecayStrategy` | 발행일에 따라 후보 점수에 가중치를 부여하는 전략 |
| 일일 추천 생성 | `RecommendationScheduler` | 매일 07:00 KST 활성 사용자 대상으로 추천 생성 |
| 추천 재생성 | `regenerateRecommendations` | 사용자가 요청하거나 프로필 갱신 후 추천을 다시 생성하는 행위 |
| 프로필 생성 이벤트 수신 | `PersonalizedProfileGeneratedEventListener` | 개인화 프로필 생성 완료 이벤트를 받아 추천 생성을 시작하는 경계 |
| 이벤트 스냅샷 추천 생성 | `generateRecommendationsForUser(user, profileVector, keyKeywords)` | ES refresh 가시성에 의존하지 않고 이벤트 payload 기반으로 추천을 생성하는 경로 |

## 용어 정합성 결정

- 표준 용어는 **`RecommendationSet`** 으로 확정한다.
- 현재 `RecommendedPost` 단건 구조는 구현상 저장 단위일 뿐, 비즈니스 개념은 “사용자별 현재 추천 목록”이다.
- `RecommendationHistory.markAsisClicked`는 오타이며 문서 표준은 `markAsClicked`다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 현재 추천 저장 단위 | `RecommendedPost` | 현재 사용자에게 노출 중인 추천 한 건 |
| 현재 추천 목록 개념 | `RecommendationSet` | 사용자의 현재 추천 목록 전체를 가리키는 표준 개념 |
| 추천 이력 레코드 | `RecommendationHistory` | 교체된 과거 추천을 보관하는 레코드 |
| 후보 모델 | `MmrCandidate` | 추천 후보 탐색 후 MMR 입력으로 쓰이는 모델 |
| 랭킹 결과 | `MmrResult` | MMR 계산 후 선택된 순위 결과 |
| 추천 제외 정책 | `createExcludeFilter(readPostIds)` | 이미 읽은 게시글을 추천에서 빼는 정책 |
| 프로필 생성 이벤트 리스너 | `PersonalizedProfileGeneratedEventListener` | Personalization Profile 이벤트를 `AFTER_COMMIT`에서 받아 추천 생성을 트리거한다 |

## 혼동 금지

- `추천 게시글`은 현재 노출 중인 결과 한 건이고, `추천 목록`은 사용자 기준 전체 묶음이다.
- `유사도 점수`와 `MMR 점수`는 다르다. 전자는 관련성, 후자는 다양성까지 반영한 선택 점수다.
- 추천의 `keyKeywords` 활용은 Search의 `SearchQuery`와 같은 것이 아니다. 하나는 프로필 파생 키워드, 다른 하나는 사용자의 직접 입력이다.
- Recommendation은 `PersonalizationProfileDocument`와 `PersonalizedProfileGeneratedEvent` payload를 소비하지만, 개인화 프로필 생성/저장 모델을 소유하지 않는다.
- 프로필 생성 직후 자동 추천은 `PersonalizedProfileGeneratedEvent` 이후 후처리이며, 프로필 저장 트랜잭션 결과와 분리한다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 추천 리스트 | 추천 목록 / RecommendationSet | 현재 추천 묶음의 표준 용어를 고정하기 위해 |
| 추천 점수 | 유사도 점수 / MMR 점수 | 점수 종류를 구분해야 한다 |
| 추천 포스트 | 추천 게시글 | Post 컨텍스트 표준 용어와 맞춘다 |
| 클릭 추천 | 추천 클릭 | 추천 결과 상호작용이라는 의미를 더 자연스럽게 드러낸다 |

## 주요 근거 파일

- `src/main/java/com/techfork/domain/recommendation/service/LlmRecommendationService.java`
- `src/main/java/com/techfork/domain/recommendation/service/MmrService.java`
- `src/main/java/com/techfork/domain/recommendation/entity/RecommendedPost.java`
- `src/main/java/com/techfork/domain/recommendation/entity/RecommendationHistory.java`
- `src/main/java/com/techfork/domain/recommendation/scheduler/RecommendationScheduler.java`
- `src/main/java/com/techfork/domain/recommendation/listener/PersonalizedProfileGeneratedEventListener.java`
