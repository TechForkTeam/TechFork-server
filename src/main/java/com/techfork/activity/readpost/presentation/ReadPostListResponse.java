package com.techfork.activity.readpost.presentation;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record ReadPostListResponse(
        List<Item> readPosts,
        Long lastReadPostId,
        boolean hasNext
) {
    @Builder
    public record Item(
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
            List<String> keywords,
            Boolean isBookmarked,
            LocalDateTime readAt
    ) {
    }
}
