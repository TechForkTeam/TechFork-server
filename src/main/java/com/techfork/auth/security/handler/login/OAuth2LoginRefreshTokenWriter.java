package com.techfork.auth.security.handler.login;

import com.techfork.auth.security.token.RefreshTokenStore;
import com.techfork.auth.security.cookie.RefreshTokenCookieWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OAuth2LoginRefreshTokenWriter {

    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    public void write(Long userId, OAuth2LoginTokens tokens, HttpServletResponse response) {
        refreshTokenStore.saveRefreshToken(userId, tokens.refreshToken(), tokens.refreshTokenExpiration());
        refreshTokenCookieWriter.write(response, tokens.refreshToken(), tokens.refreshTokenExpiration());
    }
}
