package com.techfork.auth.security.cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieWriterTest {

    private static final String DOMAIN = "localhost";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 900_000L;

    private final RefreshTokenCookieWriter refreshTokenCookieWriter = new RefreshTokenCookieWriter(DOMAIN);

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("refresh token 쿠키를 기존 wire contract로 작성한다")
        void refreshToken_AddsCookieWithExistingWireContract() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            refreshTokenCookieWriter.write(response, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);

            assertThat(response.getHeader("Set-Cookie"))
                    .contains("refreshToken=" + REFRESH_TOKEN)
                    .contains("Path=/")
                    .contains("Domain=" + DOMAIN)
                    .contains("Max-Age=900")
                    .contains("Secure")
                    .contains("HttpOnly")
                    .contains("SameSite=None");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("refresh token 쿠키 삭제 응답을 기존 wire contract로 작성한다")
        void existingRefreshToken_AddsExpiredCookieWithExistingWireContract() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            refreshTokenCookieWriter.delete(response);

            assertThat(response.getHeader("Set-Cookie"))
                    .contains("refreshToken=")
                    .contains("Path=/")
                    .contains("Domain=" + DOMAIN)
                    .contains("Max-Age=0")
                    .contains("Secure")
                    .contains("HttpOnly")
                    .contains("SameSite=None");
        }
    }

}
