package com.techfork.domain.post.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostInfoDto(
        Long id,
        String title,
        String company,
        String url,
        String logoUrl,
        LocalDateTime publishedAt
) {
}
