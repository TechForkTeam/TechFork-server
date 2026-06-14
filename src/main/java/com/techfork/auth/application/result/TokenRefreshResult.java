package com.techfork.auth.application.result;

import lombok.Builder;

@Builder
public record TokenRefreshResult(
        String accessToken,
        String refreshToken,
        long refreshTokenExpiration
) {
}
