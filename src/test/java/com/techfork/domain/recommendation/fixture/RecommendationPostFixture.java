package com.techfork.domain.recommendation.fixture;

import com.techfork.domain.source.entity.TechBlog;
import com.techfork.post.domain.Post;

import java.time.LocalDateTime;

public final class RecommendationPostFixture {

    private RecommendationPostFixture() {
    }

    public static TechBlog techBlog(String companyName, String blogUrl) {
        return TechBlog.create(
                companyName,
                blogUrl,
                blogUrl + "/rss",
                blogUrl + "/logo.png"
        );
    }

    public static Post post(TechBlog techBlog, String title, String url) {
        return post(
                techBlog,
                title,
                title + " 전체 내용",
                title + " 내용",
                title + " 요약",
                title + " 짧은 요약",
                techBlog.getBlogUrl() + "/thumb.png",
                url,
                LocalDateTime.now()
        );
    }

    public static Post post(
            TechBlog techBlog,
            String title,
            String fullContent,
            String plainContent,
            String summary,
            String shortSummary,
            String thumbnailUrl,
            String url,
            LocalDateTime publishedAt
    ) {
        return Post.builder()
                .title(title)
                .fullContent(fullContent)
                .plainContent(plainContent)
                .summary(summary)
                .shortSummary(shortSummary)
                .company(techBlog.getCompanyName())
                .logoUrl(techBlog.getLogoUrl())
                .thumbnailUrl(thumbnailUrl)
                .url(url)
                .publishedAt(publishedAt)
                .crawledAt(LocalDateTime.now())
                .techBlog(techBlog)
                .build();
    }
}
