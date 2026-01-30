package com.techfork.domain.post.dto;

import java.util.List;

public record SummaryWithKeywordsDto(
        String summary,
        String shortSummary,
        List<String> keywords
) {
}
