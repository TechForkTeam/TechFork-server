package com.techfork.recommendation.presentation.response;

import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationListResponse(
        List<RecommendedPostDto> recommendations,
        int totalCount
) {
}
