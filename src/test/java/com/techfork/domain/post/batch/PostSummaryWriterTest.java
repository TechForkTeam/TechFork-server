package com.techfork.domain.post.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.fixture.PostFixture;
import com.techfork.global.util.JdbcBatchExecutor;
import jakarta.persistence.EntityManager;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostSummaryWriterTest {

    @Mock
    private JdbcBatchExecutor jdbcBatchExecutor;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PreparedStatement updatePreparedStatement;

    @Mock
    private PreparedStatement deletePreparedStatement;

    @Mock
    private PreparedStatement firstInsertPreparedStatement;

    @Mock
    private PreparedStatement secondInsertPreparedStatement;

    @Mock
    private PreparedStatement thirdInsertPreparedStatement;

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("빈 chunk면 JDBC batch와 EntityManager clear를 수행하지 않는다")
        void doesNothingForEmptyChunk() throws Exception {
            PostSummaryWriter postSummaryWriter = createWriter();

            postSummaryWriter.write(Chunk.of());

            verify(jdbcBatchExecutor, never()).batchExecute(any(), anyList(), any());
            verify(entityManager, never()).clear();
        }

        @Test
        @DisplayName("게시글 chunk를 summary update, keyword delete, keyword insert로 위임한다")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void delegatesUpdateDeleteInsertWithExpectedBindings() throws Exception {
            PostSummaryWriter postSummaryWriter = createWriter();
            Post firstPost = createPost(1L, "첫 요약", "첫 짧은 요약", List.of("AI", "Java"));
            Post secondPost = createPost(2L, "둘 요약", "둘 짧은 요약", List.of("Spring"));
            when(jdbcBatchExecutor.batchExecute(any(), anyList(), any())).thenReturn(2);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<List> itemsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<JdbcBatchExecutor.BatchParameterSetter> setterCaptor = ArgumentCaptor.forClass(JdbcBatchExecutor.BatchParameterSetter.class);

            postSummaryWriter.write(Chunk.of(firstPost, secondPost));

            verify(jdbcBatchExecutor, times(3)).batchExecute(sqlCaptor.capture(), itemsCaptor.capture(), setterCaptor.capture());
            assertThat(sqlCaptor.getAllValues().stream().map(PostSummaryWriterTest.this::normalizeSql).toList())
                    .containsExactly(
                            "UPDATE posts SET summary = ?, short_summary = ? WHERE id = ?",
                            "DELETE FROM post_keywords WHERE post_id = ?",
                            "INSERT INTO post_keywords (keyword, post_id) VALUES (?, ?)"
                    );
            assertThat(itemsCaptor.getAllValues().get(0)).containsExactly(firstPost, secondPost);
            assertThat(itemsCaptor.getAllValues().get(1)).containsExactly(1L, 2L);
            assertThat(itemsCaptor.getAllValues().get(2)).hasSize(3);

            JdbcBatchExecutor.BatchParameterSetter<Post> updateSetter = setterCaptor.getAllValues().get(0);
            updateSetter.setValues(updatePreparedStatement, firstPost, 0);
            verify(updatePreparedStatement).setString(1, "첫 요약");
            verify(updatePreparedStatement).setString(2, "첫 짧은 요약");
            verify(updatePreparedStatement).setLong(3, 1L);

            JdbcBatchExecutor.BatchParameterSetter<Long> deleteSetter = setterCaptor.getAllValues().get(1);
            deleteSetter.setValues(deletePreparedStatement, 2L, 1);
            verify(deletePreparedStatement).setLong(1, 2L);

            JdbcBatchExecutor.BatchParameterSetter insertSetter = setterCaptor.getAllValues().get(2);
            List<?> keywordDtos = itemsCaptor.getAllValues().get(2);
            insertSetter.setValues(firstInsertPreparedStatement, keywordDtos.get(0), 0);
            verify(firstInsertPreparedStatement).setString(1, "AI");
            verify(firstInsertPreparedStatement).setLong(2, 1L);

            insertSetter.setValues(secondInsertPreparedStatement, keywordDtos.get(1), 1);
            verify(secondInsertPreparedStatement).setString(1, "Java");
            verify(secondInsertPreparedStatement).setLong(2, 1L);

            insertSetter.setValues(thirdInsertPreparedStatement, keywordDtos.get(2), 2);
            verify(thirdInsertPreparedStatement).setString(1, "Spring");
            verify(thirdInsertPreparedStatement).setLong(2, 2L);
            verify(entityManager).clear();
        }

        @Test
        @DisplayName("삽입할 keyword가 없으면 insert batch를 건너뛰고 update/delete/clear를 수행한다")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void skipsInsertWhenFlattenedKeywordListIsEmpty() throws Exception {
            PostSummaryWriter postSummaryWriter = createWriter();
            Post firstPost = createPost(1L, "첫 요약", "첫 짧은 요약", List.of());
            Post secondPost = createPost(2L, "둘 요약", "둘 짧은 요약", List.of());
            when(jdbcBatchExecutor.batchExecute(any(), anyList(), any())).thenReturn(2);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<List> itemsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<JdbcBatchExecutor.BatchParameterSetter> setterCaptor = ArgumentCaptor.forClass(JdbcBatchExecutor.BatchParameterSetter.class);

            postSummaryWriter.write(Chunk.of(firstPost, secondPost));

            verify(jdbcBatchExecutor, times(2)).batchExecute(sqlCaptor.capture(), itemsCaptor.capture(), setterCaptor.capture());
            assertThat(sqlCaptor.getAllValues().stream().map(PostSummaryWriterTest.this::normalizeSql).toList())
                    .containsExactly(
                            "UPDATE posts SET summary = ?, short_summary = ? WHERE id = ?",
                            "DELETE FROM post_keywords WHERE post_id = ?"
                    );
            assertThat(itemsCaptor.getAllValues().get(0)).containsExactly(firstPost, secondPost);
            assertThat(itemsCaptor.getAllValues().get(1)).containsExactly(1L, 2L);
            verify(entityManager).clear();
        }

        private PostSummaryWriter createWriter() {
            PostSummaryWriter postSummaryWriter = new PostSummaryWriter(jdbcBatchExecutor);
            ReflectionTestUtils.setField(postSummaryWriter, "entityManager", entityManager);
            return postSummaryWriter;
        }

        private Post createPost(Long id, String summary, String shortSummary, List<String> keywords) {
            return PostFixture.createPostWithKeywords(
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
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
