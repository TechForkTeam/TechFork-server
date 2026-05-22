package com.techfork.post.infrastructure.batch;

import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.infrastructure.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PostEmbeddingReaderTest {

    @Mock
    private PostRepository postRepository;

    @Test
    @DisplayName("생성만으로는 repository를 조회하지 않는다")
    void doesNotQueryRepositoryOnConstruction() {
        new PostEmbeddingReader(postRepository);

        verifyNoInteractions(postRepository);
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("첫 read에서만 repository를 조회하고 Post를 순차적으로 반환한다")
        void lazilyLoadsOnceAndReturnsPostsSequentially() {
            PostEmbeddingReader postEmbeddingReader = new PostEmbeddingReader(postRepository);
            Post firstPost = PostFixture.createPost(1L, "첫 번째 글", "본문1", "평문1", "TechFork", "요약1", "짧은요약1");
            Post secondPost = PostFixture.createPost(2L, "두 번째 글", "본문2", "평문2", "TechFork", "요약2", "짧은요약2");
            given(postRepository.findReadyForEmbedding()).willReturn(List.of(firstPost, secondPost));

            Post firstRead = postEmbeddingReader.read();
            Post secondRead = postEmbeddingReader.read();
            Post thirdRead = postEmbeddingReader.read();

            assertThat(firstRead).isSameAs(firstPost);
            assertThat(secondRead).isSameAs(secondPost);
            assertThat(thirdRead).isNull();
            verify(postRepository, times(1)).findReadyForEmbedding();
        }

        @Test
        @DisplayName("조회 결과가 비어 있으면 null을 반환하고 다시 조회하지 않는다")
        void returnsNullForEmptyRepositoryResultWithoutReloading() {
            PostEmbeddingReader postEmbeddingReader = new PostEmbeddingReader(postRepository);
            given(postRepository.findReadyForEmbedding()).willReturn(List.of());

            Post firstRead = postEmbeddingReader.read();
            Post secondRead = postEmbeddingReader.read();

            assertThat(firstRead).isNull();
            assertThat(secondRead).isNull();
            verify(postRepository, times(1)).findReadyForEmbedding();
        }

        @Test
        @DisplayName("새 step execution이 시작되면 repository를 다시 조회한다")
        void reloadsRepositoryWhenNewStepExecutionStarts() {
            PostEmbeddingReader postEmbeddingReader = new PostEmbeddingReader(postRepository);
            Post firstStepPost = PostFixture.createPost(1L, "첫 실행 글", "본문1", "평문1", "TechFork", "요약1", "짧은요약1");
            Post secondStepPost = PostFixture.createPost(2L, "두 번째 실행 글", "본문2", "평문2", "TechFork", "요약2", "짧은요약2");
            given(postRepository.findReadyForEmbedding())
                    .willReturn(List.of(firstStepPost))
                    .willReturn(List.of(secondStepPost));

            postEmbeddingReader.open(new ExecutionContext());
            Post firstExecutionRead = postEmbeddingReader.read();
            Post firstExecutionEnd = postEmbeddingReader.read();
            postEmbeddingReader.close();

            postEmbeddingReader.open(new ExecutionContext());
            Post secondExecutionRead = postEmbeddingReader.read();

            assertThat(firstExecutionRead).isSameAs(firstStepPost);
            assertThat(firstExecutionEnd).isNull();
            assertThat(secondExecutionRead).isSameAs(secondStepPost);
            verify(postRepository, times(2)).findReadyForEmbedding();
        }
    }
}
