package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private RefreshTokenService refreshTokenService;

    private OAuth2LoginTokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiration(REFRESH_TOKEN_EXPIRATION_MILLIS);
        tokenIssuer = new OAuth2LoginTokenIssuer(jwtUtil, jwtProperties, refreshTokenService);
        ReflectionTestUtils.setField(tokenIssuer, "domain", "localhost");
    }

    @Test
    @DisplayName("JWT 토큰을 발급하고 refresh token 저장과 cookie 설정 후 access token을 반환한다")
    void issueAccessToken_SavesRefreshTokenAndAddsCookieThenReturnsAccessToken() {
        UserPrincipal principal = principal();
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtUtil.generateTokens(USER_ID, Role.USER)).willReturn(JwtDTO.of(ACCESS_TOKEN, REFRESH_TOKEN));

        String accessToken = tokenIssuer.issueAccessToken(principal, response);

        assertThat(accessToken).isEqualTo(ACCESS_TOKEN);
        verify(jwtUtil).generateTokens(USER_ID, Role.USER);
        verify(refreshTokenService).saveRefreshToken(USER_ID, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION_MILLIS);
        assertRefreshTokenCookie(response.getHeader("Set-Cookie"));
    }

    private UserPrincipal principal() {
        return UserPrincipal.builder()
                .id(USER_ID)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .email("dev user@example.com")
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
