package com.techfork.post.application.summary;

import java.util.List;

public record SummaryExtractionResult(
        String summary,
        String shortSummary,
        List<String> keywords
) {
}
