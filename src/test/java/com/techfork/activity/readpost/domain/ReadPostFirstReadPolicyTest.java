package com.techfork.activity.readpost.domain;

import com.techfork.activity.readpost.infrastructure.FirstReadPostRepository;
import com.techfork.post.domain.Post;
import com.techfork.useraccount.domain.User;
import java.time.LocalDateTime;
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
    private FirstReadPostRepository firstReadPostRepository;

    @InjectMocks
    private ReadPostFirstReadPolicy readPostFirstReadPolicy;

    @Nested
    @DisplayName("첫 읽기 마킹")
    class MarkFirstRead {

        @Test
        @DisplayName("첫 읽기 마킹이 성공하면 true를 반환한다")
        void insertSucceeds_ReturnsTrue() {
            User user = mock(User.class);
            Post post = mock(Post.class);
            LocalDateTime readAt = LocalDateTime.of(2026, 5, 8, 12, 0);
            given(user.getId()).willReturn(1L);
            given(post.getId()).willReturn(100L);
            given(firstReadPostRepository.markFirstRead(1L, 100L, readAt)).willReturn(true);

            boolean result = readPostFirstReadPolicy.markFirstRead(user, post, readAt);

            assertThat(result).isTrue();
            verify(firstReadPostRepository, times(1)).markFirstRead(1L, 100L, readAt);
        }

        @Test
        @DisplayName("이미 마킹된 조합이면 false를 반환한다")
        void alreadyMarked_ReturnsFalse() {
            User user = mock(User.class);
            Post post = mock(Post.class);
            LocalDateTime readAt = LocalDateTime.of(2026, 5, 8, 12, 5);
            given(user.getId()).willReturn(1L);
            given(post.getId()).willReturn(100L);
            given(firstReadPostRepository.markFirstRead(1L, 100L, readAt)).willReturn(false);

            boolean result = readPostFirstReadPolicy.markFirstRead(user, post, readAt);

            assertThat(result).isFalse();
            verify(firstReadPostRepository, times(1)).markFirstRead(1L, 100L, readAt);
        }
    }
}
