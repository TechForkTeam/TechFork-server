package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtProperties;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.auth.security.oauth.UserPrincipal;
import com.techfork.auth.security.service.RefreshTokenService;
import com.techfork.auth.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginTokenIssuer {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Value("${server.domain}")
    private String domain;

    public String issueAccessToken(UserPrincipal userPrincipal, HttpServletResponse response) {
        JwtDTO tokens = jwtUtil.generateTokens(userPrincipal.getId(), userPrincipal.getRole());
        long expiration = jwtProperties.getRefreshTokenExpiration();
        refreshTokenService.saveRefreshToken(userPrincipal.getId(), tokens.refreshToken(), expiration);
        CookieUtil.addRefreshTokenCookie(response, domain, tokens.refreshToken(), expiration);
        return tokens.accessToken();
    }
}
