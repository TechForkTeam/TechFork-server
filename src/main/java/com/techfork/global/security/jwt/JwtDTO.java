package com.techfork.global.security.jwt;

public record JwtDTO(
        String accessToken,
        String refreshToken
) {
    public static JwtDTO of(String accessToken, String refreshToken) {
        return new JwtDTO(accessToken, refreshToken);
    }
}