package com.techfork.domain.source.fixture;

import com.techfork.domain.source.dto.RssFeedItem;

import java.time.LocalDateTime;

public final class RssFeedItemFixture {

    public static final LocalDateTime DEFAULT_PUBLISHED_AT = LocalDateTime.of(2026, 4, 13, 7, 0, 0);

    private RssFeedItemFixture() {
    }

    public static RssFeedItem createRssFeedItem(Long techBlogId, String title, String fullContent, String plainContent,
                                                String company, LocalDateTime publishedAt) {
        return createRssFeedItem(
                techBlogId,
                title,
                "https://posts.example.com/%s".formatted(techBlogId),
                "https://cdn.example.com/logo-%s.png".formatted(techBlogId),
                "https://cdn.example.com/thumb-%s.png".formatted(techBlogId),
                fullContent,
                plainContent,
                publishedAt,
                company
        );
    }

    public static RssFeedItem createRssFeedItem(Long techBlogId, String title, String url, String logoUrl, String thumbnailUrl,
                                                String fullContent, String plainContent, LocalDateTime publishedAt, String company) {
        return RssFeedItem.builder()
                .title(title)
                .url(url)
                .logoUrl(logoUrl)
                .thumbnailUrl(thumbnailUrl)
                .content(fullContent)
                .plainContent(plainContent)
                .publishedAt(publishedAt)
                .company(company)
                .techBlogId(techBlogId)
                .build();
    }
}
