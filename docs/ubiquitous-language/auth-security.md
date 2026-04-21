# Auth / Security

> 인증/인가 애플리케이션 서비스와 보안 인프라를 함께 다루는 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/domain/auth`
- `src/main/java/com/techfork/global/security`

## 표준 용어

| 용어 | 코드상 표현 | 정의 |
|---|---|---|
| 소셜 로그인 | `kakaoLogin`, OAuth2/OIDC | Kakao/Apple 계정을 통해 사용자를 식별하고 로그인하는 절차 |
| 액세스 토큰 | `accessToken` | API 인증용 JWT |
| 리프레시 토큰 | `refreshToken` | 액세스 토큰 재발급용 토큰. Cookie와 Redis 저장소를 사용한다. |
| 토큰 갱신 | `refreshToken` API | 리프레시 토큰을 검증하고 새 액세스/리프레시 토큰을 발급하는 행위 |
| 로그아웃 | `logout` | 리프레시 토큰을 삭제하고 쿠키를 제거하는 행위 |
| 개발자 토큰 | `DeveloperTokenResponse` | 관리자 API에서 발급하는 장기 액세스 토큰 성격의 토큰 |
| 사용자 주체 | `UserPrincipal` | Spring Security 인증 컨텍스트에서 사용자를 나타내는 객체 |

## 경계 메모

- 이 컨텍스트는 `domain/auth` **패키지만으로 닫히지 않는다.**
- JWT/OAuth/filter/config/cookie 처리의 실제 소유권은 `global/security`까지 포함한다.
- `User` aggregate 자체는 User Account 컨텍스트 소속이며, Auth / Security는 필요한 최소 사용자 식별/권한 정보만 공유한다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 토큰 발급기 | `JwtUtil` | 액세스/리프레시/개발자 토큰 생성과 검증을 담당 |
| 리프레시 토큰 저장소 | `RefreshTokenService` | Redis/Cookie 기반 리프레시 토큰 저장/검증 서비스 |
| 인증 필터 | `JwtAuthenticationFilter` | 요청에서 JWT를 읽어 인증 컨텍스트를 채우는 필터 |
| OAuth 요청 저장소 | `HttpCookieOAuth2AuthorizationRequestRepository` | OAuth 인증 요청 상태를 쿠키로 보관하는 구성요소 |
| 사용자 인증 캐시 | `UserAuthCacheService` | 로그인/토큰 갱신 이후 사용자 인증 조회를 보조하는 캐시 |

## 혼동 금지

- `개발자 토큰`은 일반 사용자 액세스 토큰과 수명/용도가 다르다.
- Auth / Security는 사용자 계정 소유 컨텍스트가 아니다. 사용자 생성/상태 전이는 User Account 컨텍스트가 소유하고, 개인화 프로필 생성은 Personalization Profile 컨텍스트가 맡는다.
- `UserPrincipal`은 인증 컨텍스트 표현이지 `User` aggregate 그 자체가 아니다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| Auth | Auth / Security | 실제 소유 범위가 `global/security`까지 포함되기 때문 |
| 유저 객체 | 사용자 주체 / `UserPrincipal` / `User` | 인증 컨텍스트 객체와 aggregate를 구분해야 한다 |
| 로그인 토큰 | 액세스 토큰 / 리프레시 토큰 | 토큰 수명과 역할을 구분해야 한다 |
| 관리자 JWT | 개발자 토큰 | 현재 운영 기능의 명칭과 맞춘다 |

## 주요 근거 파일

- `src/main/java/com/techfork/domain/auth/service/AuthService.java`
- `src/main/java/com/techfork/domain/auth/controller/AuthController.java`
- `src/main/java/com/techfork/global/security/config/SecurityConfig.java`
- `src/main/java/com/techfork/global/security/jwt/JwtUtil.java`
- `src/main/java/com/techfork/global/security/auth/service/RefreshTokenService.java`
- `src/main/java/com/techfork/global/security/oauth/UserPrincipal.java`
