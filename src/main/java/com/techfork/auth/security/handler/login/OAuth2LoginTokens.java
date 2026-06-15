package com.techfork.auth.security.handler.login;

public record OAuth2LoginTokens(
        String accessToken,
        String refreshToken,
        long refreshTokenExpiration
) {
}
