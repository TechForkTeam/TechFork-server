package com.techfork.useraccount.dto;

import lombok.Builder;

@Builder
public record AccountProfileResponse(
        String profileImage,
        String nickName,
        String email,
        String description
) {
}
