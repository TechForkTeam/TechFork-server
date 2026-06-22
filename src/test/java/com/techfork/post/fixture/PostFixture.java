package com.techfork.post.fixture;

import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.fixture.RssFeedItemFixture;
import com.techfork.domain.source.fixture.TechBlogFixture;
import com.techfork.post.domain.Post;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

public final class PostFixture {

    public static final LocalDateTime DEFAULT_PUBLISHED_AT = RssFeedItemFixture.DEFAULT_PUBLISHED_AT;
    private static final LocalDateTime DEFAULT_CRAWLED_AT = LocalDateTime.of(2026, 4, 13, 8, 0, 0);

    private PostFixture() {
    }

    public static Post createPost(Long id, String title, String fullContent, String plainContent,
                                  String company, String summary, String shortSummary) {
        TechBlog techBlog = TechBlogFixture.createTechBlog(id, company);

        Post post = Post.create(
                RssFeedItemFixture.createRssFeedItem(id, title, fullContent, plainContent, company, DEFAULT_PUBLISHED_AT),
                techBlog
        );
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "crawledAt", DEFAULT_CRAWLED_AT);
        ReflectionTestUtils.setField(post, "summary", summary);
        ReflectionTestUtils.setField(post, "shortSummary", shortSummary);
        return post;
    }

    public static Post createPost(String title, TechBlog techBlog, LocalDateTime publishedAt, Long viewCount) {
        Post post = Post.builder()
                .title(title)
                .fullContent("<p>" + title + " 내용</p>")
                .plainContent(title + " 내용")
                .company(techBlog.getCompanyName())
                .url("https://test.com/" + title)
                .logoUrl("https://test.com/logo.png")
                .thumbnailUrl("https://test.com/thumb.png")
                .publishedAt(publishedAt)
                .crawledAt(DEFAULT_CRAWLED_AT)
                .techBlog(techBlog)
                .build();
        ReflectionTestUtils.setField(post, "viewCount", viewCount);
        return post;
    }

    public static Post createPostWithKeywords(Long id, String title, String fullContent, String plainContent,
                                              String company, String summary, String shortSummary, List<String> keywords) {
        Post post = createPost(id, title, fullContent, plainContent, company, summary, shortSummary);
        post.replaceKeywords(keywords);
        return post;
    }

    public static Post createPostWithSummaryAndKeywords(Long id, String summary, String shortSummary, List<String> keywords) {
        return createPostWithKeywords(
                id,
                "게시글-" + id,
                "원문-" + id,
                "평문-" + id,
                "TechFork",
                summary,
                shortSummary,
                keywords
        );
    }

    public static Post createEmbeddingTargetPost() {
        return createPost(
                1L,
                "임베딩 대상 게시글",
                "원문 본문",
                "평문 본문",
                "TechFork",
                "요약 완료",
                "짧은 요약"
        );
    }

    public static Post createSummaryTargetPostWithExistingKeywords() {
        return createPostWithKeywords(
                1L,
                "요약 대상 글",
                "원문 본문",
                "평문 본문",
                "TechFork",
                "기존 요약",
                "기존 짧은 요약",
                List.of("기존키워드1", "기존키워드2")
        );
    }
}
