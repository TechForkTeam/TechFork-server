package com.techfork.domain.source.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.global.util.JdbcBatchExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostBatchWriterTest {

    @Mock
    private JdbcBatchExecutor jdbcBatchExecutor;

    @Mock
    private PreparedStatement preparedStatement;

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("빈 chunk면 JDBC batch 실행을 하지 않는다")
        void doesNothingForEmptyChunk() {
            PostBatchWriter postBatchWriter = new PostBatchWriter(jdbcBatchExecutor);

            postBatchWriter.write(Chunk.of());

            verify(jdbcBatchExecutor, never()).batchExecute(any(), anyList(), any());
        }

        @Test
        @DisplayName("게시글 chunk를 posts bulk insert SQL과 매핑으로 전달한다")
        @SuppressWarnings("unchecked")
        void delegatesBulkInsertWithExpectedSqlAndMappings() throws Exception {
            PostBatchWriter postBatchWriter = new PostBatchWriter(jdbcBatchExecutor);
            LocalDateTime publishedAt = LocalDateTime.of(2026, 4, 13, 6, 0, 0);
            LocalDateTime crawledAt = LocalDateTime.of(2026, 4, 13, 6, 1, 0);
            TechBlog techBlog = TechBlog.create(
                    "TechFork",
                    "https://techfork.example.com",
                    "https://techfork.example.com/rss",
                    "https://cdn.example.com/logo.png"
            );
            ReflectionTestUtils.setField(techBlog, "id", 9L);

            Post post = Post.create(RssFeedItem.builder()
                    .title("테스트 제목")
                    .url("https://posts.example.com/step3")
                    .logoUrl("https://cdn.example.com/logo.png")
                    .thumbnailUrl("https://cdn.example.com/thumb.png")
                    .content("긴 본문")
                    .plainContent("평문 본문")
                    .publishedAt(publishedAt)
                    .company("TechFork")
                    .techBlogId(9L)
                    .build(), techBlog);
            ReflectionTestUtils.setField(post, "crawledAt", crawledAt);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<List<Post>> itemsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<JdbcBatchExecutor.BatchParameterSetter<Post>> setterCaptor = ArgumentCaptor.forClass(JdbcBatchExecutor.BatchParameterSetter.class);
            given(jdbcBatchExecutor.batchExecute(any(), anyList(), any())).willReturn(1);

            postBatchWriter.write(Chunk.of(post));

            verify(jdbcBatchExecutor).batchExecute(sqlCaptor.capture(), itemsCaptor.capture(), setterCaptor.capture());
            assertThat(normalizeSql(sqlCaptor.getValue())).isEqualTo(
                    "INSERT INTO posts " +
                            "(title, full_content, plain_content, company, url, logo_url, thumbnail_url, published_at, crawled_at, view_count, tech_blog_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            assertThat(itemsCaptor.getValue()).containsExactly(post);

            JdbcBatchExecutor.BatchParameterSetter<Post> setter = setterCaptor.getValue();
            setter.setValues(preparedStatement, post, 0);

            verify(preparedStatement).setString(1, "테스트 제목");
            verify(preparedStatement).setString(2, "긴 본문");
            verify(preparedStatement).setString(3, "평문 본문");
            verify(preparedStatement).setString(4, "TechFork");
            verify(preparedStatement).setString(5, "https://posts.example.com/step3");
            verify(preparedStatement).setString(6, "https://cdn.example.com/logo.png");
            verify(preparedStatement).setString(7, "https://cdn.example.com/thumb.png");
            verify(preparedStatement).setTimestamp(8, Timestamp.valueOf(publishedAt));
            verify(preparedStatement).setTimestamp(9, Timestamp.valueOf(crawledAt));
            verify(preparedStatement).setLong(10, 0L);
            verify(preparedStatement).setLong(11, 9L);
        }
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
