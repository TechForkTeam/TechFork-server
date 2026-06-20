package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.cookie.RefreshTokenCookieWriter;
import com.techfork.auth.security.token.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginRefreshTokenWriterTest {

    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 900_000L;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private RefreshTokenCookieWriter refreshTokenCookieWriter;

    private OAuth2LoginRefreshTokenWriter refreshTokenWriter;

    @BeforeEach
    void setUp() {
        refreshTokenWriter = new OAuth2LoginRefreshTokenWriter(refreshTokenStore, refreshTokenCookieWriter);
        ReflectionTestUtils.setField(refreshTokenWriter, "domain", "localhost");
    }

    @Test
    @DisplayName("refresh token을 저장하고 cookie writer에 응답 작성을 위임한다")
    void write_SavesRefreshTokenAndAddsCookie() {
        OAuth2LoginTokens tokens = new OAuth2LoginTokens(ACCESS_TOKEN, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
        MockHttpServletResponse response = new MockHttpServletResponse();

        refreshTokenWriter.write(USER_ID, tokens, response);

        verify(refreshTokenStore).saveRefreshToken(USER_ID, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
        verify(refreshTokenCookieWriter).write(response, "localhost", REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
    }
}
