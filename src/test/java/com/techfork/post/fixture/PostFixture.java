package com.techfork.post.fixture;

import com.techfork.post.domain.Post;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

public final class PostFixture {

    public static final LocalDateTime DEFAULT_PUBLISHED_AT = LocalDateTime.of(2026, 4, 13, 7, 0, 0);
    private static final LocalDateTime DEFAULT_CRAWLED_AT = LocalDateTime.of(2026, 4, 13, 8, 0, 0);

    private PostFixture() {
    }

    public static TechBlog createTechBlog(Long id, String company) {
        TechBlog techBlog = TechBlog.create(
                company,
                "https://%s.example.com".formatted(company.toLowerCase()),
                "https://%s.example.com/rss".formatted(company.toLowerCase()),
                "https://cdn.example.com/%s.png".formatted(company.toLowerCase())
        );
        ReflectionTestUtils.setField(techBlog, "id", id);
        return techBlog;
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

    public static Post createPost(Long id, String title, String fullContent, String plainContent,
                                  String company, String summary, String shortSummary) {
        TechBlog techBlog = createTechBlog(id, company);
        RssFeedItem rssFeedItem = createRssFeedItem(id, title, fullContent, plainContent, company, DEFAULT_PUBLISHED_AT);

        Post post = Post.create(rssFeedItem, techBlog);
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "crawledAt", DEFAULT_CRAWLED_AT);
        ReflectionTestUtils.setField(post, "summary", summary);
        ReflectionTestUtils.setField(post, "shortSummary", shortSummary);
        return post;
    }

    public static Post createPostWithKeywords(Long id, String title, String fullContent, String plainContent,
                                              String company, String summary, String shortSummary, List<String> keywords) {
        Post post = createPost(id, title, fullContent, plainContent, company, summary, shortSummary);
        post.replaceKeywords(keywords);
        return post;
    }
}
