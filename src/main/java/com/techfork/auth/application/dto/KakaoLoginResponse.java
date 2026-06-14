package com.techfork.auth.application.dto;

import lombok.Builder;

@Builder
public record KakaoLoginResponse(
        String accessToken,
        Long userId,
        Boolean isRegistered
) {
}
