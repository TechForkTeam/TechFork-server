package com.techfork.post.infrastructure.row;

import lombok.Builder;

@Builder
public record CompanyRow(
        String company,
        boolean hasNewPost,
        String logoUrl
) {
}
