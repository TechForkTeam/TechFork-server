package com.techfork.post.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(name = "PostDetailResponse")
public record PostDetailDto(
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
