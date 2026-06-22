package com.techfork.auth.security.handler.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    private static final String TARGET_URL = "http://localhost:5173/login?error=true&errorCode=oauth_failed";

    @Mock
    private OAuth2LoginRedirectUrlFactory redirectUrlFactory;

    private OAuth2AuthenticationFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        failureHandler = new OAuth2AuthenticationFailureHandler(redirectUrlFactory);
    }

    @Nested
    @DisplayName("onAuthenticationFailure")
    class OnAuthenticationFailure {

        @Test
        @DisplayName("OAuth2 로그인 실패 - factory가 생성한 실패 URL로 리다이렉트한다")
        void factoryUrl_RedirectsResponse() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            BadCredentialsException exception = new BadCredentialsException("oauth_failed");
            given(redirectUrlFactory.createFailureRedirectUrl()).willReturn(TARGET_URL);

            failureHandler.onAuthenticationFailure(
                    new MockHttpServletRequest(),
                    response,
                    exception
            );

            verify(redirectUrlFactory).createFailureRedirectUrl();
            assertThat(response.getRedirectedUrl()).isEqualTo(TARGET_URL);
        }
    }

}
