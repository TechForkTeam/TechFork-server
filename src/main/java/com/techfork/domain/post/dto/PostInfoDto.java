package com.techfork.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(name = "PostInfoDto")
public record PostInfoDto(
        Long id,
        String title,
        String company,
        String url,
        String logoUrl,
        String thumbnailUrl,
        LocalDateTime publishedAt,
        Long viewCount,
        List<String> keywords
) {
}
