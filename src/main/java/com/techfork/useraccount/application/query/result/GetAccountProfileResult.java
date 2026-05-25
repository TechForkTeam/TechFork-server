package com.techfork.useraccount.application.query.result;

import lombok.Builder;

@Builder
public record GetAccountProfileResult(
        String profileImage,
        String nickName,
        String email,
        String description
) {
}
