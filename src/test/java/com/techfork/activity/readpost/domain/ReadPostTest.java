package com.techfork.activity.readpost.domain;

import com.techfork.post.domain.Post;
import com.techfork.useraccount.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReadPostTest {

    @Nested
    @DisplayName("읽은 게시글 생성")
    class Create {

        @Test
        @DisplayName("사용자, 게시글, 읽은 시각, 읽은 시간을 그대로 보존한다")
        void create_PreservesReadPostState() {
            User user = mock(User.class);
            Post post = mock(Post.class);
            LocalDateTime readAt = LocalDateTime.of(2026, 5, 5, 10, 30, 0);
            Integer readDurationSeconds = 320;

            ReadPost readPost = ReadPost.create(user, post, readAt, readDurationSeconds);

            assertThat(readPost.getUser()).isSameAs(user);
            assertThat(readPost.getPost()).isSameAs(post);
            assertThat(readPost.getReadAt()).isEqualTo(readAt);
            assertThat(readPost.getReadDurationSeconds()).isEqualTo(readDurationSeconds);
        }
    }
}
