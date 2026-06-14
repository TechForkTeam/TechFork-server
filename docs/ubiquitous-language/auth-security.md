# Auth / Security

> 인증/인가 애플리케이션 서비스와 보안 인프라를 함께 다루는 바운디드 컨텍스트입니다.

## Owning packages

- `src/main/java/com/techfork/auth`

주요 하위 패키지:

- `auth/presentation`: 인증 API 진입점
- `auth/application`: 토큰 갱신, 로그아웃, iOS 직접 로그인 등 인증 use case
- `auth/domain`: Auth / Security 전용 도메인 오류와 정책 표현
- `auth/infrastructure/kakao`: Kakao API 연동 adapter
- `auth/security`: JWT/OAuth/filter/config/cookie/cache 등 앱 전역 인증/인가 shared kernel

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

- Auth / Security의 현재 물리 경계는 최상위 `auth` 패키지다.
- `auth/security`는 Auth / Security 내부에 있지만, `UserPrincipal`, JWT 필터, OAuth 핸들러처럼 여러 컨텍스트가 기대는 앱 전역 인증/인가 shared kernel 역할을 한다.
- `User` aggregate 자체는 User Account 컨텍스트 소속이며, Auth / Security는 필요한 최소 사용자 식별/권한 정보만 공유한다.

## 내부 glossary

| 내부 용어 | 코드상 표현 | 설명 |
|---|---|---|
| 토큰 발급기 | `JwtUtil` | 액세스/리프레시/개발자 토큰 생성과 검증을 담당 |
| 리프레시 토큰 저장소 | `RefreshTokenService` | Redis/Cookie 기반 리프레시 토큰 저장/검증 서비스 |
| 인증 필터 | `JwtAuthenticationFilter` | 요청에서 JWT를 읽어 인증 컨텍스트를 채우는 필터 |
| OAuth 요청 저장소 | `HttpCookieOAuth2AuthorizationRequestRepository` | OAuth 인증 요청 상태를 쿠키로 보관하는 구성요소 |
| 사용자 인증 캐시 | `UserAuthCacheService` | 로그인/토큰 갱신 이후 사용자 인증 조회를 보조하는 캐시 |
| 인증 캐시 이벤트 리스너 | `UserAuthCacheEventListener` | User Account 이벤트를 받아 사용자 인증 캐시를 무효화하는 리스너 |

## 혼동 금지

- `개발자 토큰`은 일반 사용자 액세스 토큰과 수명/용도가 다르다.
- Auth / Security는 사용자 계정 소유 컨텍스트가 아니다. 사용자 생성/상태 전이는 User Account 컨텍스트가 소유하고, 개인화 프로필 생성은 Personalization Profile 컨텍스트가 맡는다.
- `UserPrincipal`은 인증 컨텍스트 표현이지 `User` aggregate 그 자체가 아니다.
- 온보딩 완료 캐시 무효화는 `AFTER_COMMIT` 후처리로 충분하지만, 회원 탈퇴 캐시 무효화는 보안 민감 seam이므로 `BEFORE_COMMIT`에서 실패 시 탈퇴 트랜잭션을 롤백하고 `AFTER_COMMIT`에서 한 번 더 무효화한다.

## 금지 표현 / 권장 표현

| 금지/비권장 표현 | 권장 표현 | 이유 |
|---|---|---|
| Auth | Auth / Security | 인증 API뿐 아니라 `auth/security`의 JWT/OAuth/filter/config/cookie/cache 표면까지 포함하기 때문 |
| 유저 객체 | 사용자 주체 / `UserPrincipal` / `User` | 인증 컨텍스트 객체와 aggregate를 구분해야 한다 |
| 로그인 토큰 | 액세스 토큰 / 리프레시 토큰 | 토큰 수명과 역할을 구분해야 한다 |
| 관리자 JWT | 개발자 토큰 | 현재 운영 기능의 명칭과 맞춘다 |

## 주요 근거 파일

- `src/main/java/com/techfork/auth/application/AuthService.java`
- `src/main/java/com/techfork/auth/application/KakaoLoginService.java`
- `src/main/java/com/techfork/auth/presentation/AuthController.java`
- `src/main/java/com/techfork/auth/presentation/KakaoLoginController.java`
- `src/main/java/com/techfork/auth/infrastructure/kakao/KakaoOAuthService.java`
- `src/main/java/com/techfork/auth/security/config/SecurityConfig.java`
- `src/main/java/com/techfork/auth/security/jwt/JwtUtil.java`
- `src/main/java/com/techfork/auth/security/service/RefreshTokenService.java`
- `src/main/java/com/techfork/auth/security/service/UserAuthCacheService.java`
- `src/main/java/com/techfork/auth/security/listener/UserAuthCacheEventListener.java`
- `src/main/java/com/techfork/auth/security/oauth/UserPrincipal.java`
