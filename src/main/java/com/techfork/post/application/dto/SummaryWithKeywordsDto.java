package com.techfork.post.application.dto;

import java.util.List;

public record SummaryWithKeywordsDto(
        String summary,
        String shortSummary,
        List<String> keywords
) {
}
