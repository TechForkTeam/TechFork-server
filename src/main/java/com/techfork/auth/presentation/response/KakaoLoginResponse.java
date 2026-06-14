package com.techfork.auth.presentation.response;

import lombok.Builder;

@Builder
public record KakaoLoginResponse(
        String accessToken,
        Long userId,
        Boolean isRegistered
) {
}
