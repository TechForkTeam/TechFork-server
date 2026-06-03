# Personalization Profile

> 활동 데이터와 관심사를 바탕으로 개인화 프로필을 생성하고, 검색/추천 입력 모델을 제공하는 핵심 바운디드 컨텍스트입니다.
> 현재 구현은 `personalization` 최상위 패키지로 분리되어 있으며, `User Account`와 물리적으로도 구분됩니다.

## Owning packages

- `src/main/java/com/techfork/personalization`
- read model / projection: `src/main/java/com/techfork/personalization/infrastructure`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 개인화 프로필 | `PersonalizationProfileDocument` | 사용자 활동 데이터를 LLM으로 요약하고 임베딩한 검색/추천용 projection |
| 프로필 텍스트 | `profileText` | 검색 리랭킹과 추천에 사용할 사용자 관심사 설명문 |
| 프로필 벡터 | `profileVector` | `profileText`를 임베딩한 벡터 |
| 핵심 키워드 | `keyKeywords` | LLM이 사용자 활동에서 추출한 3~5개 대표 관심 키워드 |
| 활동 데이터 | `UserActivityData` | 관심사, 최근 읽은 기술 게시글, 북마크한 기술 게시글, 검색 기록을 합친 사용자 분석 입력 |
| 프로필 재생성 | `generatePersonalizationProfile`, `generatePersonalizationProfileSync` | 활동 변화 후 개인화 프로필을 다시 만드는 행위 |
| 프로필 생성 완료 이벤트 | `PersonalizedProfileGeneratedEvent` | 개인화 프로필 projection 저장 성공 후 Recommendation 후처리에 넘기는 이벤트 |

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 프로필 생성 유스케이스 | `PersonalizationProfileService` | 개인화 프로필 생성 orchestration과 생성 완료 이벤트 발행을 맡는 application service |
| 활동 데이터 수집기 | `UserActivityCollector` | User Account, Activity, Post 신호를 모아 `UserActivityData`를 구성한다 |
| 프로필 분석기 | `PersonalizationProfileAnalyzer` | LLM 응답에서 profileText와 keyKeywords를 추출한다 |
| 프로필 생성기 | `PersonalizedProfileGenerator` | 활동 수집, LLM 분석, 임베딩, projection 저장을 조합한다 |
| 프로필 재생성 스케줄러 | `PersonalizationProfileScheduler` | 활성 사용자 개인화 프로필을 주기적으로 다시 생성하는 스케줄러 |
| 프로필 projection | `PersonalizationProfileDocument` | Elasticsearch에 저장되어 Search/Recommendation이 읽는 read model |
| 개인화 입력 키워드 | `keyKeywords` | 추천 BM25와 검색 개인화에 활용되는 키워드 |
| User Account 이벤트 리스너 | `PersonalizationProfileEventListener` | User Account 이벤트를 `AFTER_COMMIT`에서 받아 개인화 프로필 생성을 요청하는 리스너 |
| 프로필 생성 완료 이벤트 | `PersonalizedProfileGeneratedEvent` | `userId`, `profileVector`, `keyKeywords` 스냅샷을 담아 Recommendation으로 전달한다 |

## 혼동 금지

- `개인화 프로필`은 UI용 내 프로필이 아니다.
- `PersonalizationProfileDocument`는 Aggregate가 아니라 Elasticsearch projection/read model이다.
- 온보딩 완료/관심사 변경에 따른 프로필 생성은 사용자 상태 변경 트랜잭션 커밋 이후 실행된다.
- 추천 생성은 개인화 프로필 저장 성공 후 `PersonalizedProfileGeneratedEvent`를 통해 Recommendation 컨텍스트가 처리한다.
- Search/Recommendation은 `PersonalizationProfileDocument`를 읽지만, 개인화 프로필을 소유하지 않는다.
- `핵심 키워드`는 추천/검색용 프로필 파생 키워드이고, `게시글 키워드(PostKeyword)`나 `검색어(SearchQuery)`와 다르다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 프로필 | 개인화 프로필 | 계정 프로필과 구분해야 한다 |
| 사용자 프로필 문서 | 개인화 프로필 / `PersonalizationProfileDocument` | projection 성격을 분명히 하기 위해 |
| 개인화 프로필 Aggregate | 개인화 프로필 projection/read model | 현재 모델은 독립 write aggregate가 아니기 때문 |
| 검색용 프로필 | 개인화 프로필 | Search 한정 모델로 오해하지 않기 위해 |
| 관심사 프로필 | 활동 데이터 / 개인화 프로필 | 입력 데이터와 생성 결과를 구분해야 한다 |

## 주요 근거 파일

- `src/main/java/com/techfork/personalization/application/PersonalizationProfileService.java`
- `src/main/java/com/techfork/personalization/application/activity/UserActivityCollector.java`
- `src/main/java/com/techfork/personalization/application/generation/PersonalizationProfileAnalyzer.java`
- `src/main/java/com/techfork/personalization/application/generation/PersonalizedProfileGenerator.java`
- `src/main/java/com/techfork/personalization/application/event/PersonalizedProfileGeneratedEvent.java`
- `src/main/java/com/techfork/personalization/application/PersonalizationProfileEventListener.java`
- `src/main/java/com/techfork/personalization/infrastructure/PersonalizationProfileDocument.java`
- `src/main/java/com/techfork/personalization/scheduler/PersonalizationProfileScheduler.java`
