package com.techfork.post.application.batch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import co.elastic.clients.util.ObjectBuilder;
import com.techfork.post.domain.projection.ContentChunk;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.infrastructure.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostEmbeddingWriterTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private PostRepository postRepository;

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("빈 chunk면 Elasticsearch와 repository를 호출하지 않는다")
        void doesNothingForEmptyChunk() throws Exception {
            PostEmbeddingWriter postEmbeddingWriter = createWriter();

            postEmbeddingWriter.write(org.springframework.batch.item.Chunk.of());

            verify(elasticsearchClient, never()).bulk(any(Function.class));
            verify(postRepository, never()).bulkUpdateEmbeddedAt(any(), any());
        }

        @Test
        @DisplayName("문서를 bulk index 요청으로 보내고 성공한 post id들의 embeddedAt을 갱신한다")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void indexesDocumentsAndUpdatesEmbeddedAtForSuccessfulIds() throws Exception {
            PostEmbeddingWriter postEmbeddingWriter = createWriter();
            PostDocument firstDocument = createPostDocument(1L);
            PostDocument secondDocument = createPostDocument(2L);
            BulkResponse bulkResponse = BulkResponse.of(b -> b
                    .errors(false)
                    .took(1)
                    .items(
                            successItem("1"),
                            successItem("2")
                    )
            );
            doReturn(bulkResponse).when(elasticsearchClient).bulk(any(Function.class));
            ArgumentCaptor<Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>> bulkRequestCaptor =
                    ArgumentCaptor.forClass(Function.class);
            ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<LocalDateTime> embeddedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            LocalDateTime beforeWrite = LocalDateTime.now();
            postEmbeddingWriter.write(org.springframework.batch.item.Chunk.of(firstDocument, secondDocument));
            LocalDateTime afterWrite = LocalDateTime.now();

            verify(elasticsearchClient).bulk(bulkRequestCaptor.capture());
            BulkRequest bulkRequest = bulkRequestCaptor.getValue().apply(new BulkRequest.Builder()).build();
            assertThat(bulkRequest.index()).isEqualTo("posts");
            assertThat(bulkRequest.operations()).hasSize(2);
            assertThat(bulkRequest.operations())
                    .allSatisfy(operation -> assertThat(operation.isIndex()).isTrue());
            assertThat(bulkRequest.operations())
                    .extracting(operation -> operation.index().id())
                    .containsExactly("1", "2");
            assertThat(bulkRequest.operations())
                    .extracting(operation -> (PostDocument) operation.index().document())
                    .containsExactly(firstDocument, secondDocument);

            verify(postRepository).bulkUpdateEmbeddedAt(idsCaptor.capture(), embeddedAtCaptor.capture());
            assertThat(idsCaptor.getValue()).containsExactly(1L, 2L);
            assertThat(embeddedAtCaptor.getValue()).isBetween(beforeWrite, afterWrite);
        }

        @Test
        @DisplayName("bulk 일부 실패 시 성공한 post id만 embeddedAt을 갱신한다")
        void updatesOnlySuccessfulPostIdsWhenBulkHasFailures() throws Exception {
            PostEmbeddingWriter postEmbeddingWriter = createWriter();
            PostDocument firstDocument = createPostDocument(1L);
            PostDocument secondDocument = createPostDocument(2L);
            BulkResponse bulkResponse = BulkResponse.of(b -> b
                    .errors(true)
                    .took(1)
                    .items(
                            successItem("1"),
                            failureItem("2", "mapper parsing failed")
                    )
            );
            doReturn(bulkResponse).when(elasticsearchClient).bulk(any(Function.class));
            ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);

            postEmbeddingWriter.write(org.springframework.batch.item.Chunk.of(firstDocument, secondDocument));

            verify(postRepository).bulkUpdateEmbeddedAt(idsCaptor.capture(), any(LocalDateTime.class));
            assertThat(idsCaptor.getValue()).containsExactly(1L);
        }

        @Test
        @DisplayName("bulk 응답이 null이면 embeddedAt을 갱신하지 않는다")
        void skipsEmbeddedAtUpdateWhenBulkResponseIsNull() throws Exception {
            PostEmbeddingWriter postEmbeddingWriter = createWriter();
            PostDocument document = createPostDocument(1L);
            doReturn(null).when(elasticsearchClient).bulk(any(Function.class));

            postEmbeddingWriter.write(org.springframework.batch.item.Chunk.of(document));

            verify(postRepository, never()).bulkUpdateEmbeddedAt(any(), any());
        }

        @Test
        @DisplayName("성공한 문서가 없으면 embeddedAt을 갱신하지 않는다")
        void skipsEmbeddedAtUpdateWhenNoDocumentSucceeded() throws Exception {
            PostEmbeddingWriter postEmbeddingWriter = createWriter();
            PostDocument document = createPostDocument(1L);
            BulkResponse bulkResponse = BulkResponse.of(b -> b
                    .errors(true)
                    .took(1)
                    .items(failureItem("1", "write failed"))
            );
            doReturn(bulkResponse).when(elasticsearchClient).bulk(any(Function.class));

            postEmbeddingWriter.write(org.springframework.batch.item.Chunk.of(document));

            verify(postRepository, never()).bulkUpdateEmbeddedAt(any(), any());
        }

        private PostEmbeddingWriter createWriter() {
            return new PostEmbeddingWriter(elasticsearchClient, postRepository);
        }

        private PostDocument createPostDocument(Long id) {
            Post post = PostFixture.createPost(
                    id,
                    "임베딩 대상 글 " + id,
                    "원문 본문 " + id,
                    "평문 본문 " + id,
                    "TechFork",
                    "요약 완료 " + id,
                    "짧은 요약 " + id
            );
            return PostDocument.create(
                    post,
                    List.of(0.1f, 0.2f),
                    List.of(0.3f, 0.4f),
                    List.of(ContentChunk.create(0, "chunk-" + id, List.of(0.5f, 0.6f)))
            );
        }

        private BulkResponseItem successItem(String id) {
            return BulkResponseItem.of(item -> item
                    .id(id)
                    .index("posts")
                    .status(201)
                    .operationType(OperationType.Index)
            );
        }

        private BulkResponseItem failureItem(String id, String reason) {
            return BulkResponseItem.of(item -> item
                    .id(id)
                    .index("posts")
                    .status(400)
                    .operationType(OperationType.Index)
                    .error(ErrorCause.of(error -> error
                            .type("mapper_parsing_exception")
                            .reason(reason)
                    ))
            );
        }
    }
}
