package com.techfork.post.application.query.result;

import lombok.Builder;

@Builder
public record CompanyListItemResult(
        String company,
        boolean hasNewPost,
        String logoUrl
) {
}
