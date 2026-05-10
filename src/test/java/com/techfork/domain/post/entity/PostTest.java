package com.techfork.domain.post.entity;

import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("RssFeedItem과 TechBlog로 게시글 aggregate를 생성한다")
        void createsPostAggregateFromRssFeedItem() {
            TechBlog techBlog = createTechBlog();
            LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 7, 10, 30);
            RssFeedItem rssFeedItem = RssFeedItem.builder()
                    .title("Post aggregate 설계")
                    .url("https://posts.example.com/post-aggregate")
                    .logoUrl("https://cdn.example.com/logo.png")
                    .thumbnailUrl("https://cdn.example.com/thumb.png")
                    .content("원문 본문")
                    .plainContent("평문 본문")
                    .publishedAt(publishedAt)
                    .company("TechFork")
                    .techBlogId(1L)
                    .build();
            LocalDateTime beforeCreate = LocalDateTime.now();

            Post post = Post.create(rssFeedItem, techBlog);

            LocalDateTime afterCreate = LocalDateTime.now();
            assertThat(post.getTitle()).isEqualTo("Post aggregate 설계");
            assertThat(post.getFullContent()).isEqualTo("원문 본문");
            assertThat(post.getPlainContent()).isEqualTo("평문 본문");
            assertThat(post.getCompany()).isEqualTo("TechFork");
            assertThat(post.getLogoUrl()).isEqualTo("https://cdn.example.com/logo.png");
            assertThat(post.getThumbnailUrl()).isEqualTo("https://cdn.example.com/thumb.png");
            assertThat(post.getUrl()).isEqualTo("https://posts.example.com/post-aggregate");
            assertThat(post.getPublishedAt()).isEqualTo(publishedAt);
            assertThat(post.getCrawledAt()).isBetween(beforeCreate, afterCreate);
            assertThat(post.getTechBlog()).isSameAs(techBlog);
            assertThat(post.getViewCount()).isZero();
            assertThat(post.getKeywords()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateSummaries")
    class UpdateSummaries {

        @Test
        @DisplayName("summary와 shortSummary를 함께 갱신한다")
        void updatesSummaryAndShortSummaryTogether() {
            Post post = createPost();

            post.updateSummaries("새 요약", "새 짧은 요약");

            assertThat(post.getSummary()).isEqualTo("새 요약");
            assertThat(post.getShortSummary()).isEqualTo("새 짧은 요약");
        }
    }

    @Nested
    @DisplayName("replaceKeywords")
    class ReplaceKeywords {

        @Test
        @DisplayName("기존 keyword를 제거하고 새 keyword 목록으로 교체한다")
        void replacesExistingKeywordsWithNewKeywordNames() {
            Post post = createPost();
            post.replaceKeywords(List.of("Legacy", "Old"));

            post.replaceKeywords(List.of("AI", "Batch"));

            assertThat(post.getKeywords())
                    .extracting(PostKeyword::getKeyword)
                    .containsExactly("AI", "Batch");
            assertThat(post.getKeywords())
                    .allSatisfy(keyword -> assertThat(keyword.getPost()).isSameAs(post));
        }

        @Test
        @DisplayName("빈 목록이면 기존 keyword를 모두 제거한다")
        void clearsExistingKeywordsWhenKeywordNamesAreEmpty() {
            Post post = createPost();
            post.replaceKeywords(List.of("AI", "Batch"));

            post.replaceKeywords(List.of());

            assertThat(post.getKeywords()).isEmpty();
        }
    }

    private Post createPost() {
        return Post.create(
                RssFeedItem.builder()
                        .title("Post aggregate 설계")
                        .url("https://posts.example.com/post-aggregate")
                        .logoUrl("https://cdn.example.com/logo.png")
                        .thumbnailUrl("https://cdn.example.com/thumb.png")
                        .content("원문 본문")
                        .plainContent("평문 본문")
                        .publishedAt(LocalDateTime.of(2026, 5, 7, 10, 30))
                        .company("TechFork")
                        .techBlogId(1L)
                        .build(),
                createTechBlog()
        );
    }

    private TechBlog createTechBlog() {
        return TechBlog.create(
                "TechFork",
                "https://techfork.example.com",
                "https://techfork.example.com/rss",
                "https://cdn.example.com/logo.png"
        );
    }
}
