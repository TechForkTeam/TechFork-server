package com.techfork.domain.user.dto;

import lombok.Builder;

@Builder
public record UserProfileResponse(
        String profileImage,
        String nickName,
        String email,
        String description
) {
}