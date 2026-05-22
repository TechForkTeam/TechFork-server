package com.techfork.post.application.query.result;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record PostListItemResult(
        Long id,
        String title,
        String shortSummary,
        String company,
        String url,
        String logoUrl,
        String thumbnailUrl,
        LocalDateTime publishedAt,
        Long viewCount,
        List<String> keywords,
        Boolean isBookmarked
) {
}
