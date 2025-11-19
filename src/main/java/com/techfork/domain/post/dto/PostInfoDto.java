package com.techfork.domain.post.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PostInfoDto(
        Long id,
        String title,
        String company,
        String url,
        String logoUrl,
        LocalDateTime publishedAt,
        List<String> keywords
) {
}
