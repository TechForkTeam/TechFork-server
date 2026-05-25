package com.techfork.useraccount.presentation.response;

import lombok.Builder;

@Builder
public record AccountProfileResponse(
        String profileImage,
        String nickName,
        String email,
        String description
) {
}
