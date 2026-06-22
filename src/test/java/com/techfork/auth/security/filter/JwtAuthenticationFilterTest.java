package com.techfork.auth.security.filter;

import com.techfork.auth.domain.exception.AuthErrorCode;
import com.techfork.auth.security.AuthSecurityConstants;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.auth.security.cache.UserAuthCacheStore;
import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.auth.UserAuthProfile;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.techfork.auth.security.jwt.JwtConstants.TOKEN_TYPE_ACCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserAuthAccountService userAuthAccountService;

    @Mock
    private UserAuthCacheStore userAuthCacheStore;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserAuthProfile testUserProfile;
    private Long userId;
    private String validAccessToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        userId = 1L;
        validAccessToken = "valid.access.token";
        testUserProfile = new UserAuthProfile(userId, Role.USER, UserStatus.PENDING, "test@example.com", false);
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("JWT 인증 성공 - 캐시 미스: 인증 프로필 조회 후 캐시 저장")
        void cacheMiss_SetsAuthenticationAndStoresCache() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn("Bearer " + validAccessToken);
            willDoNothing().given(jwtUtil).validateToken(validAccessToken);
            given(jwtUtil.getUserIdFromToken(validAccessToken)).willReturn(userId);
            given(userAuthCacheStore.get(userId)).willReturn(null);
            given(userAuthAccountService.findAuthProfileById(userId)).willReturn(Optional.of(testUserProfile));
            given(jwtProperties.getAccessTokenExpiration()).willReturn(180000L);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            assertThat(principal.getId()).isEqualTo(userId);
            assertThat(principal.getRole()).isEqualTo(Role.USER);
            assertThat(principal.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(principal.getEmail()).isEqualTo("test@example.com");

            verify(userAuthAccountService).findAuthProfileById(userId);
            verify(userAuthCacheStore).put(eq(userId), eq(testUserProfile), eq(180000L));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 성공 - 캐시 히트: 인증 프로필 조회 없이 인증")
        void cacheHit_SetsAuthenticationWithoutProfileLookup() throws Exception {
            // Given
            UserPrincipal cachedPrincipal = UserPrincipal.builder()
                    .id(userId)
                    .role(Role.USER)
                    .status(UserStatus.ACTIVE)
                    .email("test@example.com")
                    .build();

            given(request.getHeader("Authorization")).willReturn("Bearer " + validAccessToken);
            willDoNothing().given(jwtUtil).validateToken(validAccessToken);
            given(jwtUtil.getUserIdFromToken(validAccessToken)).willReturn(userId);
            given(userAuthCacheStore.get(userId)).willReturn(cachedPrincipal);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(cachedPrincipal);

            verify(userAuthAccountService, never()).findAuthProfileById(anyLong());
            verify(userAuthCacheStore, never()).put(anyLong(), any(UserAuthProfile.class), anyLong());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 성공 - 재활성화된 PENDING 사용자는 인증 프로필 조회 후 캐시에 저장한다")
        void reactivatedPendingUserAndCacheMiss_SetsAuthenticationAndStoresCache() throws Exception {
            // Given
            UserAuthProfile reactivatedProfile = new UserAuthProfile(
                    userId,
                    Role.USER,
                    UserStatus.PENDING,
                    "reactivated@example.com",
                    false
            );

            given(request.getHeader("Authorization")).willReturn("Bearer " + validAccessToken);
            willDoNothing().given(jwtUtil).validateToken(validAccessToken);
            willDoNothing().given(jwtUtil).validateTokenType(validAccessToken, TOKEN_TYPE_ACCESS);
            given(jwtUtil.getUserIdFromToken(validAccessToken)).willReturn(userId);
            given(userAuthCacheStore.get(userId)).willReturn(null);
            given(userAuthAccountService.findAuthProfileById(userId)).willReturn(Optional.of(reactivatedProfile));
            given(jwtProperties.getAccessTokenExpiration()).willReturn(180000L);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            assertThat(principal.getId()).isEqualTo(userId);
            assertThat(principal.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(principal.getEmail()).isEqualTo("reactivated@example.com");

            verify(userAuthAccountService).findAuthProfileById(userId);
            verify(userAuthCacheStore).put(userId, reactivatedProfile, 180000L);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 실패 - Authorization 헤더 없음")
        void noAuthorizationHeader_ContinuesWithoutAuthentication() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn(null);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();

            verify(jwtUtil, never()).validateToken(anyString());
            verify(userAuthAccountService, never()).findAuthProfileById(anyLong());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 실패 - Bearer 접두사 없음")
        void noBearerPrefix_ContinuesWithoutAuthentication() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn(validAccessToken);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();

            verify(jwtUtil, never()).validateToken(anyString());
            verify(userAuthAccountService, never()).findAuthProfileById(anyLong());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 실패 - 유효하지 않은 토큰")
        void invalidToken_ClearsAuthenticationAndStoresException() throws Exception {
            // Given
            String invalidToken = "invalid.token";
            RuntimeException invalidTokenException = new RuntimeException("Invalid token");
            given(request.getHeader("Authorization")).willReturn("Bearer " + invalidToken);
            willThrow(invalidTokenException).given(jwtUtil).validateToken(invalidToken);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();

            verify(jwtUtil).validateToken(invalidToken);
            verify(userAuthAccountService, never()).findAuthProfileById(anyLong());
            verifyJwtExceptionAttribute(invalidTokenException);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 실패 - 기존 SecurityContext가 있어도 유효하지 않은 토큰이면 인증을 제거한다")
        void invalidTokenWithExistingAuthentication_ClearsAuthenticationAndStoresException() throws Exception {
            // Given
            UserPrincipal stalePrincipal = UserPrincipal.builder()
                    .id(userId)
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .email("admin@example.com")
                    .build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(stalePrincipal, null, stalePrincipal.getAuthorities())
            );

            String invalidToken = "invalid.token";
            RuntimeException invalidTokenException = new RuntimeException("Invalid token");
            given(request.getHeader("Authorization")).willReturn("Bearer " + invalidToken);
            willThrow(invalidTokenException).given(jwtUtil).validateToken(invalidToken);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();

            verify(jwtUtil).validateToken(invalidToken);
            verify(userAuthAccountService, never()).findAuthProfileById(anyLong());
            verifyJwtExceptionAttribute(invalidTokenException);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 실패 - 리프레시 토큰 사용 시도")
        void refreshTokenUsed_StoresTokenTypeMismatchException() throws Exception {
            // Given
            String refreshToken = "refresh.token";
            RuntimeException tokenTypeMismatchException = new RuntimeException("Token type mismatch");
            given(request.getHeader("Authorization")).willReturn("Bearer " + refreshToken);
            willDoNothing().given(jwtUtil).validateToken(refreshToken);
            doThrow(tokenTypeMismatchException)
                    .when(jwtUtil).validateTokenType(refreshToken, TOKEN_TYPE_ACCESS);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();

            verify(jwtUtil).validateToken(refreshToken);
            verify(jwtUtil).validateTokenType(refreshToken, TOKEN_TYPE_ACCESS);
            verify(userAuthAccountService, never()).findAuthProfileById(anyLong());
            verifyJwtExceptionAttribute(tokenTypeMismatchException);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 실패 - 사용자를 찾을 수 없음 (삭제된 사용자)")
        void userNotFound_StoresUserNotFoundException() throws Exception {
            // Given
            String deletedUserToken = "deleted.user.token";
            given(request.getHeader("Authorization")).willReturn("Bearer " + deletedUserToken);
            willDoNothing().given(jwtUtil).validateToken(deletedUserToken);
            willDoNothing().given(jwtUtil).validateTokenType(deletedUserToken, TOKEN_TYPE_ACCESS);
            given(jwtUtil.getUserIdFromToken(deletedUserToken)).willReturn(999L);
            given(userAuthAccountService.findAuthProfileById(999L)).willReturn(Optional.empty());

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();

            verify(jwtUtil).validateToken(deletedUserToken);
            verify(jwtUtil).validateTokenType(deletedUserToken, TOKEN_TYPE_ACCESS);
            verify(userAuthAccountService).findAuthProfileById(999L);
            verifyJwtExceptionAttribute(AuthErrorCode.USER_NOT_FOUND);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 실패 - 탈퇴한 회원 (캐시 미스 후 인증 프로필 조회)")
        void withdrawnUserAndCacheMiss_StoresWithdrawnUserExceptionWithoutCaching() throws Exception {
            // Given
            UserAuthProfile withdrawnProfile = new UserAuthProfile(
                    userId,
                    Role.USER,
                    UserStatus.WITHDRAWN,
                    null,
                    false
            );

            given(request.getHeader("Authorization")).willReturn("Bearer " + validAccessToken);
            willDoNothing().given(jwtUtil).validateToken(validAccessToken);
            willDoNothing().given(jwtUtil).validateTokenType(validAccessToken, TOKEN_TYPE_ACCESS);
            given(jwtUtil.getUserIdFromToken(validAccessToken)).willReturn(userId);
            given(userAuthCacheStore.get(userId)).willReturn(null);
            given(userAuthAccountService.findAuthProfileById(userId)).willReturn(Optional.of(withdrawnProfile));

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();

            verify(userAuthAccountService).findAuthProfileById(userId);
            verify(userAuthCacheStore, never()).put(anyLong(), any(UserAuthProfile.class), anyLong()); // 탈퇴 유저는 캐시 저장 안 함
            verifyJwtExceptionAttribute(AuthErrorCode.WITHDRAWN_USER);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("JWT 인증 실패 - 탈퇴한 회원 (캐시 히트)")
        void withdrawnUserAndCacheHit_StoresWithdrawnUserException() throws Exception {
            // Given
            UserPrincipal withdrawnPrincipal = UserPrincipal.builder()
                    .id(userId)
                    .role(Role.USER)
                    .status(UserStatus.WITHDRAWN)
                    .email(null)
                    .build();

            given(request.getHeader("Authorization")).willReturn("Bearer " + validAccessToken);
            willDoNothing().given(jwtUtil).validateToken(validAccessToken);
            willDoNothing().given(jwtUtil).validateTokenType(validAccessToken, TOKEN_TYPE_ACCESS);
            given(jwtUtil.getUserIdFromToken(validAccessToken)).willReturn(userId);
            given(userAuthCacheStore.get(userId)).willReturn(withdrawnPrincipal);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();

            verify(userAuthAccountService, never()).findAuthProfileById(anyLong()); // 캐시 히트이므로 인증 프로필 조회 없음
            verifyJwtExceptionAttribute(AuthErrorCode.WITHDRAWN_USER);
            verify(filterChain).doFilter(request, response);
        }
    }

    private void verifyJwtExceptionAttribute(Exception expectedException) {
        verify(request).setAttribute(AuthSecurityConstants.JWT_EXCEPTION_ATTRIBUTE, expectedException);
    }

    private void verifyJwtExceptionAttribute(AuthErrorCode expectedErrorCode) {
        verify(request).setAttribute(eq(AuthSecurityConstants.JWT_EXCEPTION_ATTRIBUTE), argThat(exception ->
                exception instanceof GeneralException generalException
                        && generalException.getCode() == expectedErrorCode
        ));
    }
}
