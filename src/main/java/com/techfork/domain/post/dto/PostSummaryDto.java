package com.techfork.domain.post.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostSummaryDto(
        Long id,
        String title,
        String summary,
        String company,
        String url,
        LocalDateTime publishedAt,
        LocalDateTime crawledAt
) {
}
