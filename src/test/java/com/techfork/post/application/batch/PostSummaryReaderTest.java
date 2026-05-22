package com.techfork.post.application.batch;

import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.infrastructure.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PostSummaryReaderTest {

    @Mock
    private PostRepository postRepository;

    @Test
    @DisplayName("생성만으로는 repository를 조회하지 않는다")
    void doesNotQueryRepositoryOnConstruction() {
        new PostSummaryReader(postRepository);

        verifyNoInteractions(postRepository);
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("첫 read에서만 repository를 조회하고 Post를 순차적으로 반환한다")
        void lazilyLoadsOnceAndReturnsPostsSequentially() {
            PostSummaryReader postSummaryReader = new PostSummaryReader(postRepository);
            Post firstPost = PostFixture.createPost(1L, "첫 번째 글", "본문1", "평문1", "TechFork", null, null);
            Post secondPost = PostFixture.createPost(2L, "두 번째 글", "본문2", "평문2", "TechFork", null, null);
            given(postRepository.findWithKeywordsBySummaryIsNull()).willReturn(List.of(firstPost, secondPost));

            Post firstRead = postSummaryReader.read();
            Post secondRead = postSummaryReader.read();
            Post thirdRead = postSummaryReader.read();

            assertThat(firstRead).isSameAs(firstPost);
            assertThat(secondRead).isSameAs(secondPost);
            assertThat(thirdRead).isNull();
            verify(postRepository, times(1)).findWithKeywordsBySummaryIsNull();
        }

        @Test
        @DisplayName("조회 결과가 비어 있으면 null을 반환하고 다시 조회하지 않는다")
        void returnsNullForEmptyRepositoryResultWithoutReloading() {
            PostSummaryReader postSummaryReader = new PostSummaryReader(postRepository);
            given(postRepository.findWithKeywordsBySummaryIsNull()).willReturn(List.of());

            Post firstRead = postSummaryReader.read();
            Post secondRead = postSummaryReader.read();

            assertThat(firstRead).isNull();
            assertThat(secondRead).isNull();
            verify(postRepository, times(1)).findWithKeywordsBySummaryIsNull();
        }
    }
}
