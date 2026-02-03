package com.techfork.domain.recommendation.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RecommendedPostDto(
        Long id,
        Long postId,
        String title,
        String shortSummary,
        String company,
        String url,
        String logoUrl,
        String thumbnailUrl,
        Long viewCount,
        Boolean isBookmarked,
        LocalDateTime publishedAt,
        List<String> keywords,
        Double similarityScore,
        Double mmrScore,
        Integer rank,
        LocalDateTime recommendedAt
) {
    public RecommendedPostDto withBookmarkStatus(boolean isBookmarked) {
        return new RecommendedPostDto(
                id,
                postId,
                title,
                shortSummary,
                company,
                url,
                logoUrl,
                thumbnailUrl,
                viewCount,
                isBookmarked,
                publishedAt,
                keywords,
                similarityScore,
                mmrScore,
                rank,
                recommendedAt
        );
    }
}
