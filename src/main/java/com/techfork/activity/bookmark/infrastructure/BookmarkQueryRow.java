package com.techfork.activity.bookmark.infrastructure;

import java.time.LocalDateTime;

public record BookmarkQueryRow(
        Long bookmarkId,
        Long postId,
        String title,
        String shortSummary,
        String url,
        String companyName,
        String logoUrl,
        LocalDateTime publishedAt,
        String thumbnailUrl,
        Long viewCount
) {
}
