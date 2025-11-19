package com.techfork.domain.post.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PostDetailDto(
        Long id,
        String title,
        String summary,
        String company,
        String url,
        String logoUrl,
        LocalDateTime publishedAt,
        List<String> keywords
) {
}
