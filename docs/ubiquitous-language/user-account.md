# User Account

> 사용자 계정, 온보딩, 관심사, 계정 프로필을 다루는 개념적 바운디드 컨텍스트입니다.  
> 현재 구현 패키지는 `domain/user`에 함께 존재하지만, 전략 문서에서는 `Personalization Profile`과 분리해서 봅니다.

## Owning packages

- `src/main/java/com/techfork/domain/user`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 사용자 | `User` | 서비스를 사용하는 주체 |
| 소셜 사용자 | `createSocialUser`, `SocialType` | Kakao/Apple 등 외부 인증 제공자로부터 생성된 사용자 |
| 소셜 타입 | `SocialType` | `KAKAO`, `APPLE` |
| 회원 상태 | `UserStatus` | `PENDING`, `ACTIVE`, `WITHDRAWN` |
| 대기 사용자 | `PENDING` | 온보딩 미완료 상태 |
| 활성 사용자 | `ACTIVE` | 온보딩 완료 상태 |
| 탈퇴 사용자 | `WITHDRAWN` | 탈퇴 완료 상태. 개인정보는 null 처리된다. |
| 온보딩 | `completeOnboarding` | 닉네임, 이메일, 설명, 관심사를 저장하고 사용자를 활성화하는 절차 |
| 관심 카테고리 | `EInterestCategory`, `UserInterestCategory` | 사용자가 선택한 기술 분야 |
| 관심 키워드 | `EInterestKeyword`, `UserInterestKeyword` | 카테고리에 속한 구체 기술 키워드 |
| 계정 프로필 | `nickName`, `description`, `profileImage` | 사용자에게 보이는 기본 프로필 정보 |

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 사용자 루트 | `User` | 계정/상태/기본 프로필/관심사를 소유하는 aggregate root |
| 온보딩 완료 커맨드 | `UserCommandService.completeOnboarding` | 계정 정보 저장 + 관심사 저장 + 활성화 흐름 |
| 관심사 교체 커맨드 | `InterestCommandService.updateUserInterests` | 관심사 전체를 갈아끼우는 흐름 |
| 계정 프로필 수정 | `UserCommandService.updateUserProfile` | 닉네임/자기소개 수정 흐름 |
| 탈퇴 처리 | `User.withdraw()` | 개인정보 익명화와 상태 변경을 수행하는 도메인 동작 |

## 혼동 금지

- `계정 프로필`은 UI에서 보이는 사용자 정보이고, `개인화 프로필`은 검색/추천 입력 모델이다.
- `관심사`는 User Account가 소유하는 입력 데이터다. 다만 그 데이터는 Personalization Profile 생성의 재료로도 사용된다.
- Auth / Security가 다루는 `UserPrincipal`은 User Account aggregate 그 자체가 아니다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| 프로필 | 계정 프로필 | 개인화 프로필과 구분해야 한다 |
| 유저 정보 | 사용자 계정 정보 | 계정/인증/개인화 의미를 분리하기 위해 |
| 온보딩 프로필 | 온보딩 / 계정 프로필 | 온보딩 절차와 프로필 개념을 분리하기 위해 |
| 관심사 프로필 | 관심사 | 입력 데이터와 파생 모델을 섞지 않기 위해 |

## 주요 근거 파일

- `src/main/java/com/techfork/domain/user/entity/User.java`
- `src/main/java/com/techfork/domain/user/enums/UserStatus.java`
- `src/main/java/com/techfork/domain/user/enums/EInterestCategory.java`
- `src/main/java/com/techfork/domain/user/enums/EInterestKeyword.java`
- `src/main/java/com/techfork/domain/user/service/UserCommandService.java`
- `src/main/java/com/techfork/domain/user/service/InterestCommandService.java`
- `src/main/java/com/techfork/domain/user/controller/OnboardingController.java`
- `src/main/java/com/techfork/domain/user/controller/UserController.java`
