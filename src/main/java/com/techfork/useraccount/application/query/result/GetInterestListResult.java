package com.techfork.useraccount.application.query.result;

import lombok.Builder;

import java.util.List;

@Builder
public record GetInterestListResult(
        List<CategoryResult> categories
) {
    @Builder
    public record CategoryResult(
            String category,
            String displayName,
            List<KeywordResult> keywords
    ) {
    }

    public record KeywordResult(
            String keyword,
            String displayName
    ) {
    }
}
