package com.techfork.post.infrastructure.batch;

import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PostEmbeddingReaderDataJpaTest {

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
        @DisplayName("요약이 공백이 아니고 embeddedAt이 null인 게시글만 읽는다")
        void readsOnlyPostsReadyForEmbedding() throws Exception {
            Post readyPost1 = savePost("ready-1", "요약 완료 1", "짧은 요약 1", null);
            Post readyPost2 = savePost("ready-2", "요약 완료 2", "짧은 요약 2", null);
            savePost("null-summary", null, null, null);
            savePost("empty-summary", "", "", null);
            savePost("blank-summary", "   ", "   ", null);
            savePost("already-embedded", "이미 임베딩됨", "짧은 요약", LocalDateTime.of(2026, 5, 11, 9, 0));

            entityManager.clear();

            PostEmbeddingReader postEmbeddingReader = new PostEmbeddingReader(postRepository);
            List<Post> readPosts = new ArrayList<>();

            Post firstRead = postEmbeddingReader.read();
            Post secondRead = postEmbeddingReader.read();
            Post thirdRead = postEmbeddingReader.read();

            readPosts.add(firstRead);
            readPosts.add(secondRead);

            assertThat(thirdRead).isNull();
            assertThat(readPosts)
                    .extracting(Post::getId)
                    .containsExactlyInAnyOrder(readyPost1.getId(), readyPost2.getId());
            assertThat(readPosts)
                    .allSatisfy(post -> {
                        assertThat(post.getSummary()).isNotBlank();
                        assertThat(post.getEmbeddedAt()).isNull();
                    });
        }

        @Test
        @DisplayName("조건을 만족하는 게시글이 없으면 null을 반환한다")
        void returnsNullWhenNoPostsAreReadyForEmbedding() throws Exception {
            savePost("null-summary", null, null, null);
            savePost("empty-summary", "", "", null);
            savePost("blank-summary", "   ", "   ", null);
            savePost("already-embedded", "이미 임베딩됨", "짧은 요약", LocalDateTime.of(2026, 5, 11, 9, 0));

            entityManager.clear();

            PostEmbeddingReader postEmbeddingReader = new PostEmbeddingReader(postRepository);

            assertThat(postEmbeddingReader.read()).isNull();
        }
    }

    private Post savePost(String suffix, String summary, String shortSummary, LocalDateTime embeddedAt) {
        Post post = Post.create(
                RssFeedItem.builder()
                        .title("임베딩 대상 글 " + suffix)
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
        ReflectionTestUtils.setField(post, "embeddedAt", embeddedAt);
        return postRepository.saveAndFlush(post);
    }
}
