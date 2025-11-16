package com.techfork.domain.post.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostDetailDto(
        Long id,
        String title,
        String fullContent,
        String plainContent,
        String summary,
        String company,
        String url,
        LocalDateTime publishedAt,
        LocalDateTime crawledAt,
        TechBlogInfo techBlog
) {
    @Builder
    public record TechBlogInfo(
            Long id,
            String companyName,
            String blogUrl,
            String logoUrl
    ) {
    }
}
