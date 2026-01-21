package com.techfork.domain.source.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RssFeedItem(
        String title,
        String url,
        String logoUrl,
        String thumbnailUrl,
        String content,
        String plainContent,
        LocalDateTime publishedAt,
        String company,
        Long techBlogId
) {
}
