package com.techfork.post.application.command;

import com.techfork.post.infrastructure.PostRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostViewCountCommandServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostViewCountCommandService postViewCountCommandService;

    @Nested
    @DisplayName("incrementViewCount")
    class IncrementViewCount {

        @Test
        @DisplayName("1건이 업데이트되면 true를 반환한다")
        void singleRowUpdated_ReturnsTrue() {
            Long postId = 100L;
            given(postRepository.incrementViewCount(postId)).willReturn(1);

            boolean incremented = postViewCountCommandService.incrementViewCount(postId);

            assertThat(incremented).isTrue();
            verify(postRepository, times(1)).incrementViewCount(postId);
        }

        @Test
        @DisplayName("업데이트 건수가 1이 아니면 false를 반환한다")
        void updatedCountIsNotOne_ReturnsFalse() {
            Long postId = 100L;
            given(postRepository.incrementViewCount(postId)).willReturn(0);

            boolean incremented = postViewCountCommandService.incrementViewCount(postId);

            assertThat(incremented).isFalse();
            verify(postRepository, times(1)).incrementViewCount(postId);
        }
    }
}
