package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 900_000L;
    private static final String REDIRECT_URI = "http://localhost:5173/auth/callback?registered=%s&token=%s&email=%s";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private Authentication authentication;

    private JwtProperties jwtProperties;
    private OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiration(REFRESH_TOKEN_EXPIRATION_MILLIS);
        jwtProperties.setRedirectUri(REDIRECT_URI);

        successHandler = new OAuth2AuthenticationSuccessHandler(jwtUtil, jwtProperties, refreshTokenService);
        ReflectionTestUtils.setField(successHandler, "domain", "localhost");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - ACTIVE 사용자는 등록 완료 상태로 토큰과 함께 리다이렉트한다")
    void onAuthenticationSuccess_ActiveUser_RedirectsWithRegisteredTrueAndTokens() throws Exception {
        UserPrincipal principal = principal(UserStatus.ACTIVE, "dev user@example.com");
        given(authentication.getPrincipal()).willReturn(principal);
        given(jwtUtil.generateTokens(USER_ID, Role.USER)).willReturn(JwtDTO.of(ACCESS_TOKEN, REFRESH_TOKEN));

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        verify(jwtUtil).generateTokens(USER_ID, Role.USER);
        verify(refreshTokenService).saveRefreshToken(USER_ID, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
        assertThat(response.getRedirectedUrl()).isEqualTo(String.format(
                REDIRECT_URI,
                true,
                ACCESS_TOKEN,
                UriUtils.encode("dev user@example.com", StandardCharsets.UTF_8)
        ));
        assertRefreshTokenCookie(response.getHeader("Set-Cookie"));
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - PENDING 사용자는 등록 미완료 상태로 리다이렉트한다")
    void onAuthenticationSuccess_PendingUser_RedirectsWithRegisteredFalse() throws Exception {
        UserPrincipal principal = principal(UserStatus.PENDING, "pending@example.com");
        given(authentication.getPrincipal()).willReturn(principal);
        given(jwtUtil.generateTokens(USER_ID, Role.USER)).willReturn(JwtDTO.of(ACCESS_TOKEN, REFRESH_TOKEN));

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(String.format(
                REDIRECT_URI,
                false,
                ACCESS_TOKEN,
                UriUtils.encode("pending@example.com", StandardCharsets.UTF_8)
        ));
        verify(refreshTokenService).saveRefreshToken(USER_ID, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
        assertRefreshTokenCookie(response.getHeader("Set-Cookie"));
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - 이메일이 없으면 빈 이메일 파라미터로 리다이렉트한다")
    void onAuthenticationSuccess_NullEmail_RedirectsWithEmptyEmail() throws Exception {
        UserPrincipal principal = principal(UserStatus.ACTIVE, null);
        given(authentication.getPrincipal()).willReturn(principal);
        given(jwtUtil.generateTokens(USER_ID, Role.USER)).willReturn(JwtDTO.of(ACCESS_TOKEN, REFRESH_TOKEN));

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(String.format(
                REDIRECT_URI,
                true,
                ACCESS_TOKEN,
                ""
        ));
        verify(refreshTokenService).saveRefreshToken(USER_ID, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
        assertRefreshTokenCookie(response.getHeader("Set-Cookie"));
    }

    private UserPrincipal principal(UserStatus status, String email) {
        return UserPrincipal.builder()
                .id(USER_ID)
                .role(Role.USER)
                .status(status)
                .email(email)
                .build();
    }

    private void assertRefreshTokenCookie(String setCookieHeader) {
        assertThat(setCookieHeader)
                .contains("refreshToken=" + REFRESH_TOKEN)
                .contains("Path=/")
                .contains("Domain=localhost")
                .contains("Max-Age=900")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None");
    }
}
