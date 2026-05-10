package com.techfork.domain.post.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.global.util.JdbcBatchExecutor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JdbcBatchExecutor.class)
class PostSummaryWriterDataJpaTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private JdbcBatchExecutor jdbcBatchExecutor;

    @Autowired
    private EntityManager entityManager;

    private PostSummaryWriter postSummaryWriter;
    private TechBlog techBlog;

    @BeforeEach
    void setUp() {
        postSummaryWriter = new PostSummaryWriter(jdbcBatchExecutor);
        ReflectionTestUtils.setField(postSummaryWriter, "entityManager", entityManager);

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
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("summary를 갱신하고 기존 keyword를 제거한 뒤 새 keyword만 저장한다")
        void updatesSummariesAndReplacesPersistedKeywords() {
            Post post = savePost("기존 요약", "기존 짧은 요약", List.of("Legacy", "Old"));

            entityManager.clear();

            Post processedPost = postRepository.findById(post.getId()).orElseThrow();
            processedPost.updateSummaries("새 요약", "새 짧은 요약");
            processedPost.replaceKeywords(List.of("AI", "Batch"));

            postSummaryWriter.write(Chunk.of(processedPost));

            entityManager.clear();

            Post reloadedPost = postRepository.findById(post.getId()).orElseThrow();

            assertThat(reloadedPost.getSummary()).isEqualTo("새 요약");
            assertThat(reloadedPost.getShortSummary()).isEqualTo("새 짧은 요약");
            assertThat(findKeywordNames(post.getId())).containsExactlyInAnyOrder("AI", "Batch");
        }

        @Test
        @DisplayName("새 keyword가 없으면 기존 keyword를 모두 삭제하고 summary만 저장한다")
        void deletesExistingKeywordsWhenNewKeywordListIsEmpty() {
            Post post = savePost("기존 요약", "기존 짧은 요약", List.of("Legacy", "Old"));

            entityManager.clear();

            Post processedPost = postRepository.findById(post.getId()).orElseThrow();
            processedPost.updateSummaries("새 요약", "새 짧은 요약");
            processedPost.replaceKeywords(List.of());

            postSummaryWriter.write(Chunk.of(processedPost));

            entityManager.clear();

            Post reloadedPost = postRepository.findById(post.getId()).orElseThrow();

            assertThat(reloadedPost.getSummary()).isEqualTo("새 요약");
            assertThat(reloadedPost.getShortSummary()).isEqualTo("새 짧은 요약");
            assertThat(findKeywordNames(post.getId())).isEmpty();
        }
    }

    private Post savePost(String summary, String shortSummary, List<String> keywords) {
        Post post = Post.create(
                RssFeedItem.builder()
                        .title("요약 대상 글")
                        .url("https://posts.example.com/post-summary-writer")
                        .logoUrl("https://cdn.example.com/post-summary-writer-logo.png")
                        .thumbnailUrl("https://cdn.example.com/post-summary-writer-thumb.png")
                        .content("원문 본문")
                        .plainContent("평문 본문")
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

    @SuppressWarnings("unchecked")
    private List<String> findKeywordNames(Long postId) {
        return entityManager.createNativeQuery("""
                        SELECT keyword
                        FROM post_keywords
                        WHERE post_id = :postId
                        ORDER BY keyword
                        """)
                .setParameter("postId", postId)
                .getResultList();
    }
}
