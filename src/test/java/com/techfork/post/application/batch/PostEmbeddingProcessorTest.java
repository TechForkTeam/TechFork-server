package com.techfork.post.application.batch;

import com.techfork.post.domain.projection.ContentChunk;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.application.support.ContentChunkerService;
import com.techfork.global.llm.EmbeddingClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PostEmbeddingProcessorTest {

    private static final LocalDateTime DEFAULT_PUBLISHED_AT = LocalDateTime.of(2026, 4, 13, 7, 0, 0);

    @Mock
    private ContentChunkerService contentChunkerService;

    @Mock
    private EmbeddingClient embeddingClient;

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("제목, 요약, 유효 본문 청크를 임베딩해 PostDocument projection을 생성한다")
        void createsPostDocumentProjectionFromEmbeddings() {
            PostEmbeddingProcessor postEmbeddingProcessor = createProcessor();
            Post post = createPost();
            List<Float> titleEmbedding = List.of(0.1f, 0.2f);
            List<Float> summaryEmbedding = List.of(0.3f, 0.4f);
            List<String> rawChunks = List.of("첫 번째 청크", "", "  ", "두 번째 청크");
            List<List<Float>> chunkEmbeddings = List.of(
                    List.of(1.1f, 1.2f),
                    List.of(2.1f, 2.2f)
            );

            given(embeddingClient.embed("임베딩 대상 게시글")).willReturn(titleEmbedding);
            given(embeddingClient.embed("요약 완료")).willReturn(summaryEmbedding);
            given(contentChunkerService.chunkContent("원문 본문")).willReturn(rawChunks);
            given(embeddingClient.embedBatch(List.of("첫 번째 청크", "두 번째 청크"))).willReturn(chunkEmbeddings);

            PostDocument result = postEmbeddingProcessor.process(post);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("1");
            assertThat(result.getPostId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("임베딩 대상 게시글");
            assertThat(result.getSummary()).isEqualTo("요약 완료");
            assertThat(result.getShortSummary()).isEqualTo("짧은 요약");
            assertThat(result.getCompany()).isEqualTo("TechFork");
            assertThat(result.getUrl()).isEqualTo("https://posts.example.com/1");
            assertThat(result.getLogoUrl()).isEqualTo("https://cdn.example.com/logo-1.png");
            assertThat(result.getThumbnailUrl()).isEqualTo("https://cdn.example.com/thumb-1.png");
            assertThat(result.getPublishedAt()).isEqualTo(DEFAULT_PUBLISHED_AT);
            assertThat(result.getTitleEmbedding()).containsExactlyElementsOf(titleEmbedding);
            assertThat(result.getSummaryEmbedding()).containsExactlyElementsOf(summaryEmbedding);
            assertThat(result.getContentChunks()).hasSize(2);
            assertThat(result.getContentChunks())
                    .extracting(ContentChunk::getChunkOrder, ContentChunk::getChunkText, ContentChunk::getEmbedding)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(0, "첫 번째 청크", List.of(1.1f, 1.2f)),
                            org.assertj.core.groups.Tuple.tuple(1, "두 번째 청크", List.of(2.1f, 2.2f))
                    );

            verify(embeddingClient).embed("임베딩 대상 게시글");
            verify(embeddingClient).embed("요약 완료");
            verify(contentChunkerService).chunkContent("원문 본문");
            verify(embeddingClient).embedBatch(List.of("첫 번째 청크", "두 번째 청크"));
        }

        @Test
        @DisplayName("제목이 비어 있으면 임베딩을 스킵하고 null을 반환한다")
        void returnsNullWhenTitleIsBlank() {
            PostEmbeddingProcessor postEmbeddingProcessor = createProcessor();
            Post post = createPost();
            ReflectionTestUtils.setField(post, "title", " ");

            PostDocument result = postEmbeddingProcessor.process(post);

            assertThat(result).isNull();
            verifyNoInteractions(embeddingClient, contentChunkerService);
        }

        @Test
        @DisplayName("요약이 비어 있으면 임베딩을 스킵하고 null을 반환한다")
        void returnsNullWhenSummaryIsBlank() {
            PostEmbeddingProcessor postEmbeddingProcessor = createProcessor();
            Post post = createPost();
            ReflectionTestUtils.setField(post, "summary", " ");

            PostDocument result = postEmbeddingProcessor.process(post);

            assertThat(result).isNull();
            verifyNoInteractions(embeddingClient, contentChunkerService);
        }

        @Test
        @DisplayName("유효한 본문 청크가 없으면 batch embedding 없이 null을 반환한다")
        void returnsNullWhenNoValidChunksRemain() {
            PostEmbeddingProcessor postEmbeddingProcessor = createProcessor();
            Post post = createPost();
            given(embeddingClient.embed("임베딩 대상 게시글")).willReturn(List.of(0.1f));
            given(embeddingClient.embed("요약 완료")).willReturn(List.of(0.2f));
            given(contentChunkerService.chunkContent("원문 본문"))
                    .willReturn(Arrays.asList("", " ", null));

            PostDocument result = postEmbeddingProcessor.process(post);

            assertThat(result).isNull();
            verify(embeddingClient).embed("임베딩 대상 게시글");
            verify(embeddingClient).embed("요약 완료");
            verify(contentChunkerService).chunkContent("원문 본문");
            verify(embeddingClient, never()).embedBatch(anyList());
        }

        @Test
        @DisplayName("임베딩 클라이언트 예외를 그대로 전파한다")
        void propagatesEmbeddingClientFailure() {
            PostEmbeddingProcessor postEmbeddingProcessor = createProcessor();
            Post post = createPost();
            given(embeddingClient.embed("임베딩 대상 게시글"))
                    .willThrow(new IllegalStateException("embedding failed"));

            assertThatThrownBy(() -> postEmbeddingProcessor.process(post))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("embedding failed");
            verify(embeddingClient).embed("임베딩 대상 게시글");
            verify(embeddingClient, never()).embed("요약 완료");
            verifyNoInteractions(contentChunkerService);
        }

        private PostEmbeddingProcessor createProcessor() {
            return new PostEmbeddingProcessor(contentChunkerService, embeddingClient);
        }

        private Post createPost() {
            return PostFixture.createPost(
                    1L,
                    "임베딩 대상 게시글",
                    "원문 본문",
                    "평문 본문",
                    "TechFork",
                    "요약 완료",
                    "짧은 요약"
            );
        }
    }
}
