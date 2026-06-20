package com.techfork.auth.security.handler.login;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginRefreshTokenIssuerTest {

    private static final Long USER_ID = 1L;
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 900_000L;

    @Mock
    private JwtUtil jwtUtil;

    private OAuth2LoginRefreshTokenIssuer refreshTokenIssuer;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiration(REFRESH_TOKEN_EXPIRATION_MILLIS);
        refreshTokenIssuer = new OAuth2LoginRefreshTokenIssuer(jwtUtil, jwtProperties);
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 경로에서 refresh token만 발급한다")
    void issue_ReturnsOAuth2LoginRefreshTokenWithoutSideEffects() {
        UserPrincipal principal = principal();
        given(jwtUtil.generateRefreshToken(USER_ID, Role.USER)).willReturn(REFRESH_TOKEN);

        OAuth2LoginRefreshToken issuedRefreshToken = refreshTokenIssuer.issue(principal);

        assertThat(issuedRefreshToken.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(issuedRefreshToken.refreshTokenExpiration()).isEqualTo(REFRESH_TOKEN_EXPIRATION_MILLIS);
        verify(jwtUtil).generateRefreshToken(USER_ID, Role.USER);
        verify(jwtUtil, never()).generateTokens(USER_ID, Role.USER);
    }

    private UserPrincipal principal() {
        return UserPrincipal.builder()
                .id(USER_ID)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .email("dev user@example.com")
                .build();
    }
}
