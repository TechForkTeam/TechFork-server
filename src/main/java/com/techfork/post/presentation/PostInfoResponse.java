package com.techfork.post.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Schema(name = "PostInfoResponse")
public record PostInfoResponse(
        Long id,
        String title,
        String shortSummary,
        String company,
        String url,
        String logoUrl,
        String thumbnailUrl,
        LocalDateTime publishedAt,
        Long viewCount,
        List<String> keywords,
        Boolean isBookmarked
) {
}
