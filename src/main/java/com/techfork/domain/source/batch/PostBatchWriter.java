package com.techfork.domain.source.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.global.util.JdbcBatchExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Post를 배치로 저장하는 Writer
 * Processor에서 중복 체크가 완료되므로 여기서는 단순 저장만 수행
 * JDBC Bulk Insert를 사용하여 성능 최적화
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostBatchWriter implements ItemWriter<Post> {

    private final JdbcBatchExecutor jdbcBatchExecutor;

    private static final String INSERT_SQL = """
            INSERT INTO posts
            (title, full_content, plain_content, company, url, logo_url, thumbnail_url, published_at, crawled_at, view_count, tech_blog_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Override
    public void write(Chunk<? extends Post> chunk) {
        if (chunk.isEmpty()) {
            return;
        }

        List<? extends Post> items = chunk.getItems();

        int inserted = jdbcBatchExecutor.batchExecute(INSERT_SQL, items, (ps, post, i) -> {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getFullContent());
            ps.setString(3, post.getPlainContent());
            ps.setString(4, post.getCompany());
            ps.setString(5, post.getUrl());
            ps.setString(6, post.getLogoUrl());
            ps.setString(7, post.getThumbnailUrl());
            ps.setTimestamp(8, JdbcBatchExecutor.toTimestamp(post.getPublishedAt()));
            ps.setTimestamp(9, JdbcBatchExecutor.toTimestamp(post.getCrawledAt()));
            ps.setLong(10, 0L);
            ps.setLong(11, post.getTechBlog().getId());
        });

        log.info("{}개 게시글 Bulk Insert 완료", inserted);
    }
}
