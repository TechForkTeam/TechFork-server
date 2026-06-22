package com.techfork.auth.security.jwt;

import com.techfork.useraccount.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.techfork.auth.security.jwt.JwtConstants.TOKEN_TYPE_REFRESH;
import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final Long USER_ID = 1L;
    private static final String SECRET = "test-jwt-secret-key-for-jwt-util-test-must-be-at-least-256-bits";
    private static final long ACCESS_TOKEN_EXPIRATION_MILLIS = 900_000L;
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 1_209_600_000L;

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("refresh token만 발급하면 refresh 타입 토큰을 생성한다")
        void userIdAndRole_ReturnsRefreshTypeToken() {
            JwtUtil jwtUtil = jwtUtil();

            String refreshToken = jwtUtil.generateRefreshToken(USER_ID, Role.USER);

            assertThat(jwtUtil.getUserIdFromToken(refreshToken)).isEqualTo(USER_ID);
            assertThat(jwtUtil.getTokenType(refreshToken)).isEqualTo(TOKEN_TYPE_REFRESH);
        }
    }

    private JwtUtil jwtUtil() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProperties.setAccessTokenExpiration(ACCESS_TOKEN_EXPIRATION_MILLIS);
        jwtProperties.setRefreshTokenExpiration(REFRESH_TOKEN_EXPIRATION_MILLIS);
        return new JwtUtil(jwtProperties);
    }
}
