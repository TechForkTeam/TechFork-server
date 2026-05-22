package com.techfork.post.infrastructure.batch;

import com.techfork.post.domain.Post;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.post.infrastructure.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PostSummaryReaderDataJpaTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private EntityManager entityManager;

    private TechBlog techBlog;

    @BeforeEach
    void setUp() {
        techBlog = techBlogRepository.save(
                TechBlog.create(
                        "TechFork",
                        "https://techfork.example.com",
                        "https://techfork.example.com/rss",
                        "https://cdn.example.com/logo.png"
                )
        );
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("summary가 null이거나 빈 문자열인 게시글만 읽는다")
        void readsOnlyPostsWithNullOrEmptySummary() throws Exception {
            Post nullSummaryPost = savePost("null-summary", null, null, List.of("AI"));
            Post emptySummaryPost = savePost("empty-summary", "", "", List.of("Batch"));
            savePost("completed-summary", "완료 요약", "완료 짧은 요약", List.of("Done"));

            entityManager.clear();

            PostSummaryReader postSummaryReader = new PostSummaryReader(postRepository);
            List<Post> readPosts = new ArrayList<>();

            Post firstRead = postSummaryReader.read();
            Post secondRead = postSummaryReader.read();
            Post thirdRead = postSummaryReader.read();

            readPosts.add(firstRead);
            readPosts.add(secondRead);

            assertThat(thirdRead).isNull();
            assertThat(readPosts)
                    .extracting(Post::getId)
                    .containsExactlyInAnyOrder(nullSummaryPost.getId(), emptySummaryPost.getId());

            PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
            assertThat(readPosts)
                    .allSatisfy(post -> assertThat(persistenceUnitUtil.isLoaded(post, "keywords")).isTrue());
            assertThat(readPosts.stream()
                    .filter(post -> post.getId().equals(nullSummaryPost.getId()))
                    .findFirst()
                    .orElseThrow()
                    .getKeywords())
                    .extracting(keyword -> keyword.getKeyword())
                    .containsExactly("AI");
            assertThat(readPosts.stream()
                    .filter(post -> post.getId().equals(emptySummaryPost.getId()))
                    .findFirst()
                    .orElseThrow()
                    .getKeywords())
                    .extracting(keyword -> keyword.getKeyword())
                    .containsExactly("Batch");
        }

        @Test
        @DisplayName("summary가 있는 게시글만 있으면 null을 반환한다")
        void returnsNullWhenNoPostsMatchSummaryCondition() throws Exception {
            savePost("completed-summary", "완료 요약", "완료 짧은 요약", List.of("Done"));

            entityManager.clear();

            PostSummaryReader postSummaryReader = new PostSummaryReader(postRepository);

            assertThat(postSummaryReader.read()).isNull();
        }
    }

    private Post savePost(String suffix, String summary, String shortSummary, List<String> keywords) {
        Post post = Post.create(
                RssFeedItem.builder()
                        .title("요약 대상 글 " + suffix)
                        .url("https://posts.example.com/" + suffix)
                        .logoUrl("https://cdn.example.com/logo-" + suffix + ".png")
                        .thumbnailUrl("https://cdn.example.com/thumb-" + suffix + ".png")
                        .content("원문 본문 " + suffix)
                        .plainContent("평문 본문 " + suffix)
                        .publishedAt(LocalDateTime.of(2026, 5, 10, 10, 0))
                        .company("TechFork")
                        .techBlogId(techBlog.getId())
                        .build(),
                techBlog
        );
        post.updateSummaries(summary, shortSummary);
        post.replaceKeywords(keywords);
        return postRepository.saveAndFlush(post);
    }
}
