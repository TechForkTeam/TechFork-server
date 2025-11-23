package com.techfork.domain.recommendation.dto;

import com.techfork.domain.post.dto.PostInfoDto;
import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationListResponse(
        List<RecommendedPostDto> recommendations,
        int totalCount
) {
}
