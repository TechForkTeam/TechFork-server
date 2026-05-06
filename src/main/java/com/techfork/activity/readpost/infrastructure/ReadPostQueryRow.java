package com.techfork.activity.readpost.infrastructure;

import java.time.LocalDateTime;

public record ReadPostQueryRow(
        Long readPostId,
        Long postId,
        String title,
        String shortSummary,
        String url,
        String companyName,
        String logoUrl,
        LocalDateTime publishedAt,
        String thumbnailUrl,
        Long viewCount,
        LocalDateTime readAt
) {
}
