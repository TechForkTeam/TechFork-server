package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.token.RefreshTokenStore;
import com.techfork.auth.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OAuth2LoginRefreshTokenWriter {

    private final RefreshTokenStore refreshTokenStore;

    @Value("${server.domain}")
    private String domain;

    public void write(Long userId, OAuth2LoginTokens tokens, HttpServletResponse response) {
        refreshTokenStore.saveRefreshToken(userId, tokens.refreshToken(), tokens.refreshTokenExpiration());
        CookieUtil.addRefreshTokenCookie(response, domain, tokens.refreshToken(), tokens.refreshTokenExpiration());
    }
}
