package com.techfork.post.infrastructure.row;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PostDetailRow(
        Long id,
        String title,
        String summary,
        String company,
        String url,
        String logoUrl,
        LocalDateTime publishedAt,
        Long viewCount,
        List<String> keywords,
        Boolean isBookmarked
) {
}
