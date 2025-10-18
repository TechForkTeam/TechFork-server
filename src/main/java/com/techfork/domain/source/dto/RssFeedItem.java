package com.techfork.domain.source.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RssFeedItem(
        String title,
        String url,
        String content,
        LocalDateTime publishedAt,
        String company,
        Long techBlogId,
        String tags
) {
}
