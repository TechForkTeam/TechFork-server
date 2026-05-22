package com.techfork.domain.recommendation.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationListResponse(
        List<RecommendedPostDto> recommendations,
        int totalCount
) {
}
