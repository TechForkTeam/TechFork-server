package com.techfork.activity.bookmark.application.query;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BookmarkDto(
        Long bookmarkId,
        Long postId,
        String title,
        String shortSummary,
        String url,
        String companyName,
        String logoUrl,
        LocalDateTime publishedAt,
        String thumbnailUrl,
        Long viewCount,
        List<String> keywords,
        Boolean isBookmarked
) {
}
