package com.techfork.auth.security.handler.login;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 900_000L;
    private static final String TARGET_URL = "http://localhost:5173/auth/callback";

    @Mock
    private OAuth2LoginTokenIssuer tokenIssuer;

    @Mock
    private OAuth2LoginRefreshTokenWriter refreshTokenWriter;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2LoginRedirectUrlFactory redirectUrlFactory;

    private OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2AuthenticationSuccessHandler(tokenIssuer, refreshTokenWriter, redirectUrlFactory);
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - ACTIVE 사용자는 토큰 발급/refresh 저장 후 factory가 생성한 URL로 리다이렉트한다")
    void onAuthenticationSuccess_ActiveUser_RedirectsToFactoryUrlWithTokens() throws Exception {
        UserPrincipal principal = principal(UserStatus.ACTIVE, "dev user@example.com");
        OAuth2LoginTokens tokens = tokens();
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(authentication.getPrincipal()).willReturn(principal);
        given(tokenIssuer.issue(principal)).willReturn(tokens);
        given(redirectUrlFactory.createSuccessRedirectUrl(principal, ACCESS_TOKEN)).willReturn(TARGET_URL);

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        verify(tokenIssuer).issue(principal);
        verify(refreshTokenWriter).write(USER_ID, tokens, response);
        verify(redirectUrlFactory).createSuccessRedirectUrl(principal, ACCESS_TOKEN);
        assertThat(response.getRedirectedUrl()).isEqualTo(TARGET_URL);
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - PENDING 사용자도 토큰 발급/refresh 저장 후 factory가 생성한 URL로 리다이렉트한다")
    void onAuthenticationSuccess_PendingUser_RedirectsToFactoryUrl() throws Exception {
        UserPrincipal principal = principal(UserStatus.PENDING, "pending@example.com");
        OAuth2LoginTokens tokens = tokens();
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(authentication.getPrincipal()).willReturn(principal);
        given(tokenIssuer.issue(principal)).willReturn(tokens);
        given(redirectUrlFactory.createSuccessRedirectUrl(principal, ACCESS_TOKEN)).willReturn(TARGET_URL);

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        verify(tokenIssuer).issue(principal);
        verify(refreshTokenWriter).write(USER_ID, tokens, response);
        verify(redirectUrlFactory).createSuccessRedirectUrl(principal, ACCESS_TOKEN);
        assertThat(response.getRedirectedUrl()).isEqualTo(TARGET_URL);
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - 이메일이 없어도 토큰 발급/refresh 저장과 redirect URL 생성을 위임한다")
    void onAuthenticationSuccess_NullEmail_DelegatesTokenIssuingRefreshWritingAndRedirectUrlCreation() throws Exception {
        UserPrincipal principal = principal(UserStatus.ACTIVE, null);
        OAuth2LoginTokens tokens = tokens();
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(authentication.getPrincipal()).willReturn(principal);
        given(tokenIssuer.issue(principal)).willReturn(tokens);
        given(redirectUrlFactory.createSuccessRedirectUrl(principal, ACCESS_TOKEN)).willReturn(TARGET_URL);

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        verify(tokenIssuer).issue(principal);
        verify(refreshTokenWriter).write(USER_ID, tokens, response);
        verify(redirectUrlFactory).createSuccessRedirectUrl(principal, ACCESS_TOKEN);
        assertThat(response.getRedirectedUrl()).isEqualTo(TARGET_URL);
    }

    private OAuth2LoginTokens tokens() {
        return new OAuth2LoginTokens(ACCESS_TOKEN, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
    }

    private UserPrincipal principal(UserStatus status, String email) {
        return UserPrincipal.builder()
                .id(USER_ID)
                .role(Role.USER)
                .status(status)
                .email(email)
                .build();
    }
}
