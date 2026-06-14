package com.techfork.global.security.handler.login;

import com.techfork.auth.security.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2AuthenticationFailureHandlerTest {

    private static final String LOGIN_FAILURE_REDIRECT_URI = "http://localhost:5173/login?error=true";

    private OAuth2AuthenticationFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setLoginFailureRedirectUri(LOGIN_FAILURE_REDIRECT_URI);
        failureHandler = new OAuth2AuthenticationFailureHandler(jwtProperties);
    }

    @Test
    @DisplayName("OAuth2 로그인 실패 - 실패 리다이렉트 URI에 에러 메시지를 query parameter로 추가한다")
    void onAuthenticationFailure_RedirectsWithErrorQueryParameter() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("oauth_failed")
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo(LOGIN_FAILURE_REDIRECT_URI + "&error=oauth_failed");
    }
}
