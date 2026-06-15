package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.token.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginRefreshTokenWriterTest {

    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 900_000L;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private OAuth2LoginRefreshTokenWriter refreshTokenWriter;

    @BeforeEach
    void setUp() {
        refreshTokenWriter = new OAuth2LoginRefreshTokenWriter(refreshTokenStore);
        ReflectionTestUtils.setField(refreshTokenWriter, "domain", "localhost");
    }

    @Test
    @DisplayName("refresh token을 저장하고 기존 cookie 정책으로 응답에 설정한다")
    void write_SavesRefreshTokenAndAddsCookie() {
        OAuth2LoginTokens tokens = new OAuth2LoginTokens(ACCESS_TOKEN, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
        MockHttpServletResponse response = new MockHttpServletResponse();

        refreshTokenWriter.write(USER_ID, tokens, response);

        verify(refreshTokenStore).saveRefreshToken(USER_ID, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
        assertRefreshTokenCookie(response.getHeader("Set-Cookie"));
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
