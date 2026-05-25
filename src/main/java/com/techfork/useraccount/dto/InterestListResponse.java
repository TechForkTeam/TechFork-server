package com.techfork.useraccount.dto;

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
    ) {}

    public record Keyword(
            String keyword,
            String displayName
    ) {}
}
