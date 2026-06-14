package com.techfork.auth.application.command.result;

import lombok.Builder;

@Builder
public record KakaoLoginResult(
        String accessToken,
        String refreshToken,
        long refreshTokenExpiration,
        Long userId,
        Boolean isRegistered
) {
}
