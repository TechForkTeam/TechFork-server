package com.techfork.auth.application.command.result;

import lombok.Builder;

@Builder
public record TokenRefreshResult(
        String accessToken,
        String refreshToken,
        long refreshTokenExpiration
) {
}
