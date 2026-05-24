package com.techfork.post.application.query.lookup;

import com.techfork.global.exception.GeneralException;
import com.techfork.post.domain.Post;
import com.techfork.post.domain.exception.PostErrorCode;
import com.techfork.post.infrastructure.PostRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PostLookupServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostLookupService postLookupService;

    @Nested
    @DisplayName("게시글 조회")
    class GetPostOrThrow {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("존재하는 게시글이면 반환한다")
            void getPostOrThrow_Success() {
                Long postId = 100L;
                Post post = mock(Post.class);
                given(postRepository.findById(postId)).willReturn(Optional.of(post));

                Post result = postLookupService.getPostOrThrow(postId);

                assertThat(result).isSameAs(post);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 게시글이면 예외를 던진다")
            void getPostOrThrow_Fail_PostNotFound() {
                Long postId = 999L;
                given(postRepository.findById(postId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> postLookupService.getPostOrThrow(postId))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", PostErrorCode.POST_NOT_FOUND);
            }
        }
    }
}
