package com.techfork.activity.bookmark.presentation;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BookmarkListResponse(
        List<Item> bookmarks,
        Long lastBookmarkId,
        boolean hasNext
) {
    @Builder
    public record Item(
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
}
