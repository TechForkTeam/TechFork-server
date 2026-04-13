package com.techfork.domain.source.fixture;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public final class SourcePostFixture {

    private SourcePostFixture() {
    }

    public static Post createPost(Long id, String title, String fullContent, String plainContent,
                                  String company, String summary, String shortSummary) {
        TechBlog techBlog = TechBlog.create(
                company,
                "https://%s.example.com".formatted(company.toLowerCase()),
                "https://%s.example.com/rss".formatted(company.toLowerCase()),
                "https://cdn.example.com/%s.png".formatted(company.toLowerCase())
        );
        ReflectionTestUtils.setField(techBlog, "id", id);

        Post post = Post.create(RssFeedItem.builder()
                .title(title)
                .url("https://posts.example.com/%s".formatted(id))
                .logoUrl("https://cdn.example.com/logo-%s.png".formatted(id))
                .thumbnailUrl("https://cdn.example.com/thumb-%s.png".formatted(id))
                .content(fullContent)
                .plainContent(plainContent)
                .publishedAt(LocalDateTime.of(2026, 4, 13, 7, 0, 0))
                .company(company)
                .techBlogId(id)
                .build(), techBlog);
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "summary", summary);
        ReflectionTestUtils.setField(post, "shortSummary", shortSummary);
        return post;
    }
}
