package com.techfork.domain.post.dto;

import java.util.List;

public record SummaryWithKeywords(
        String summary,
        List<String> keywords
) {
}
