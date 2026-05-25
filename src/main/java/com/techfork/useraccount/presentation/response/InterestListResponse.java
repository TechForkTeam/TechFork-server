package com.techfork.useraccount.presentation.response;

import lombok.Builder;

import java.util.List;

@Builder
public record InterestListResponse(
        List<Category> categories
) {
    @Builder
    public record Category(
            String category,
            String displayName,
            List<Keyword> keywords
    ) {
    }

    public record Keyword(
            String keyword,
            String displayName
    ) {
    }
}
