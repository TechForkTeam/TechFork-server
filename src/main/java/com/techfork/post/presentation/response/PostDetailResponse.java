package com.techfork.post.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(name = "PostDetailResponse")
public record PostDetailResponse(
        Long id,
        String title,
        String summary,
        String company,
        String url,
        String logoUrl,
        LocalDateTime publishedAt,
        Long viewCount,
        List<String> keywords,
        Boolean isBookmarked
) {
}
