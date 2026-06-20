package com.techfork.auth.security.handler.login;

record OAuth2LoginRefreshToken(
        String refreshToken,
        long refreshTokenExpiration
) {
}
