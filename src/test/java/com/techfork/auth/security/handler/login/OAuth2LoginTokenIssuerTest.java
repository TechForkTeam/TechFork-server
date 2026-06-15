package com.techfork.auth.security.handler.login;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginTokenIssuerTest {

    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 900_000L;

    @Mock
    private JwtUtil jwtUtil;

    private OAuth2LoginTokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiration(REFRESH_TOKEN_EXPIRATION_MILLIS);
        tokenIssuer = new OAuth2LoginTokenIssuer(jwtUtil, jwtProperties);
    }

    @Test
    @DisplayName("JWT 토큰을 발급하고 refresh token 만료 시간을 포함한 OAuth2 로그인 토큰을 반환한다")
    void issue_ReturnsOAuth2LoginTokensWithoutSideEffects() {
        UserPrincipal principal = principal();
        given(jwtUtil.generateTokens(USER_ID, Role.USER)).willReturn(JwtDTO.of(ACCESS_TOKEN, REFRESH_TOKEN));

        OAuth2LoginTokens tokens = tokenIssuer.issue(principal);

        assertThat(tokens.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(tokens.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(tokens.refreshTokenExpiration()).isEqualTo(REFRESH_TOKEN_EXPIRATION_MILLIS);
        verify(jwtUtil).generateTokens(USER_ID, Role.USER);
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
