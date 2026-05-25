package com.techfork.activity.bookmark.domain;

import com.techfork.post.domain.Post;
import com.techfork.useraccount.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BookmarkTest {

    @Nested
    @DisplayName("북마크 생성")
    class Create {

        @Test
        @DisplayName("사용자, 게시글, 북마크 시각을 그대로 보존한다")
        void create_PreservesBookmarkState() {
            User user = mock(User.class);
            Post post = mock(Post.class);
            LocalDateTime bookmarkedAt = LocalDateTime.of(2026, 5, 5, 10, 30, 0);

            Bookmark bookmark = Bookmark.create(user, post, bookmarkedAt);

            assertThat(bookmark.getUser()).isSameAs(user);
            assertThat(bookmark.getPost()).isSameAs(post);
            assertThat(bookmark.getBookmarkedAt()).isEqualTo(bookmarkedAt);
        }
    }
}
