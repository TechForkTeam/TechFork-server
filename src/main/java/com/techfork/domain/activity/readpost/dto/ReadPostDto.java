package com.techfork.domain.activity.readpost.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ReadPostDto(
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
