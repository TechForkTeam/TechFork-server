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

    public void write(Long userId, OAuth2LoginRefreshToken issuedRefreshToken, HttpServletResponse response) {
        refreshTokenStore.saveRefreshToken(
                userId,
                issuedRefreshToken.refreshToken(),
                issuedRefreshToken.refreshTokenExpiration()
        );
        refreshTokenCookieWriter.write(
                response,
                issuedRefreshToken.refreshToken(),
                issuedRefreshToken.refreshTokenExpiration()
        );
    }
}
