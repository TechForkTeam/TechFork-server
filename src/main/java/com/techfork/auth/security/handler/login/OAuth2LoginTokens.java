package com.techfork.auth.security.handler.login;

record OAuth2LoginTokens(
        String accessToken,
        String refreshToken,
        long refreshTokenExpiration
) {
}
