package com.techfork.auth.security.handler.login;

record OAuth2LoginTokens(
        String refreshToken,
        long refreshTokenExpiration
) {
}
