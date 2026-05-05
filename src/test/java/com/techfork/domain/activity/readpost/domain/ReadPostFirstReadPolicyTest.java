package com.techfork.domain.activity.readpost.domain;

import com.techfork.domain.activity.readpost.repository.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.useraccount.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadPostFirstReadPolicyTest {

    @Mock
    private ReadPostRepository readPostRepository;

    @InjectMocks
    private ReadPostFirstReadPolicy readPostFirstReadPolicy;

    @Nested
    @DisplayName("첫 읽기 판별")
    class IsFirstRead {

        @Test
        @DisplayName("기존 읽기 기록이 없으면 true를 반환한다")
        void isFirstRead_ReturnTrue_WhenNoExistingReadPost() {
            User user = mock(User.class);
            Post post = mock(Post.class);
            given(readPostRepository.existsByUserAndPost(user, post)).willReturn(false);

            boolean result = readPostFirstReadPolicy.isFirstRead(user, post);

            assertThat(result).isTrue();
            verify(readPostRepository, times(1)).existsByUserAndPost(user, post);
        }

        @Test
        @DisplayName("기존 읽기 기록이 있으면 false를 반환한다")
        void isFirstRead_ReturnFalse_WhenExistingReadPostExists() {
            User user = mock(User.class);
            Post post = mock(Post.class);
            given(readPostRepository.existsByUserAndPost(user, post)).willReturn(true);

            boolean result = readPostFirstReadPolicy.isFirstRead(user, post);

            assertThat(result).isFalse();
            verify(readPostRepository, times(1)).existsByUserAndPost(user, post);
        }
    }
}
