# Personalization Profile

> 활동 데이터와 관심사를 바탕으로 개인화 프로필을 생성하고, 검색/추천 입력 모델을 제공하는 개념적 바운디드 컨텍스트입니다.  
> 현재 구현 패키지는 `domain/user`에 함께 존재하지만, 전략 문서에서는 `User Account`와 분리해서 봅니다.

## Owning packages

- `src/main/java/com/techfork/domain/user`
- 관련 read model: `src/main/java/com/techfork/domain/user/document`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 개인화 프로필 | `UserProfileDocument` | 사용자 활동 데이터를 LLM으로 요약하고 임베딩한 개인화용 프로필 문서 |
| 프로필 텍스트 | `profileText` | 검색 리랭킹과 추천에 사용할 사용자 관심사 설명문 |
| 프로필 벡터 | `profileVector` | `profileText`를 임베딩한 벡터 |
| 핵심 키워드 | `keyKeywords` | LLM이 사용자 활동에서 추출한 3~5개 대표 관심 키워드 |
| 활동 데이터 | `UserActivityData` | 관심사, 최근 읽은 기술 게시글, 북마크한 기술 게시글, 검색 기록을 합친 사용자 분석 입력 |
| 프로필 재생성 | `generateUserProfile`, `generateUserProfileSync` | 활동 변화 후 개인화 프로필을 다시 만드는 행위 |

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 프로필 생성 서비스 | `UserProfileService` | 활동/관심사 데이터를 모아 개인화 프로필 projection을 생성하는 서비스 |
| 프로필 재생성 스케줄러 | `UserProfileScheduler` | 활성 사용자 개인화 프로필을 주기적으로 다시 생성하는 스케줄러 |
| 프로필 projection | `UserProfileDocument` | 검색/추천에 제공되는 read model |
| 개인화 입력 키워드 | `keyKeywords` | 추천 BM25와 검색 개인화에 활용되는 키워드 |
| 프로필 생성 트리거 | 관심사 변경 / 활동 누적 | 현재는 서비스 직접 호출 기반, 장기적으로는 이벤트 분리 대상 |

## 혼동 금지

- `개인화 프로필`은 UI용 내 프로필이 아니다.
- `UserProfileDocument`는 aggregate라기보다 projection/read model이다.
- `핵심 키워드`는 추천/검색용 프로필 파생 키워드이고, `게시글 키워드(PostKeyword)`나 `검색어(SearchQuery)`와 다르다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 프로필 | 개인화 프로필 | 계정 프로필과 구분해야 한다 |
| 사용자 프로필 문서 | 개인화 프로필 / `UserProfileDocument` | projection 성격을 분명히 하기 위해 |
| 검색용 프로필 | 개인화 프로필 | Search 한정 모델로 오해하지 않기 위해 |
| 관심사 프로필 | 활동 데이터 / 개인화 프로필 | 입력 데이터와 생성 결과를 구분해야 한다 |

## 주요 근거 파일

- `src/main/java/com/techfork/domain/user/service/UserProfileService.java`
- `src/main/java/com/techfork/domain/user/document/UserProfileDocument.java`
- `src/main/java/com/techfork/domain/user/scheduler/UserProfileScheduler.java`
