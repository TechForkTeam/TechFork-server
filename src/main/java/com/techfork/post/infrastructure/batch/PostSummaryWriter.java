package com.techfork.post.infrastructure.batch;

import com.techfork.post.domain.Post;
import com.techfork.post.domain.PostKeyword;
import com.techfork.global.util.JdbcBatchExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 요약이 추가된 Post를 저장하는 Writer
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostSummaryWriter implements ItemWriter<Post> {

    private final JdbcBatchExecutor jdbcBatchExecutor;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void write(Chunk<? extends Post> chunk) {
        List<? extends Post> posts = chunk.getItems();
        if (posts.isEmpty()) {
            return;
        }

        updatePostSummaries(posts);
        deleteOldKeywords(posts);
        insertNewKeywords(posts);

        log.info("PostSummaryWriter: {}개 게시글 처리 완료", posts.size());

        entityManager.clear();
    }

    private void updatePostSummaries(List<? extends Post> posts) {
        String sql = "UPDATE posts SET summary = ?, short_summary = ? WHERE id = ?";

        @SuppressWarnings("unchecked")
        List<Post> postList = (List<Post>) posts;

        int totalUpdated = jdbcBatchExecutor.batchExecute(sql, postList, (ps, post, i) -> {
            ps.setString(1, post.getSummary());
            ps.setString(2, post.getShortSummary());
            ps.setLong(3, post.getId());
        });

        log.debug("UPDATE posts: {}개 업데이트", totalUpdated);
    }

    private void deleteOldKeywords(List<? extends Post> posts) {
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        if (postIds.isEmpty()) {
            return;
        }

        String sql = "DELETE FROM post_keywords WHERE post_id = ?";

        int deletedCount = jdbcBatchExecutor.batchExecute(sql, postIds, (ps, id, i) ->
            ps.setLong(1, id)
        );
        log.debug("DELETE post_keywords: {}개 삭제", deletedCount);
    }

    private void insertNewKeywords(List<? extends Post> posts) {
        // Post에서 모든 PostKeyword를 평탄화
        List<KeywordInsertDto> keywordDtos = new ArrayList<>();
        for (Post post : posts) {
            for (PostKeyword keyword : post.getKeywords()) {
                keywordDtos.add(new KeywordInsertDto(keyword.getKeyword(), post.getId()));
            }
        }

        if (keywordDtos.isEmpty()) {
            log.debug("INSERT post_keywords: 삽입할 키워드 없음");
            return;
        }

        String sql = "INSERT INTO post_keywords (keyword, post_id) VALUES (?, ?)";

        int inserted = jdbcBatchExecutor.batchExecute(sql, keywordDtos, (ps, dto, i) -> {
            ps.setString(1, dto.keyword);
            ps.setLong(2, dto.postId);
        });

        log.debug("INSERT post_keywords: {}개 삽입", inserted);
    }

    /**
     * 키워드 삽입을 위한 DTO
     */
    private record KeywordInsertDto(String keyword, Long postId) {
    }
}