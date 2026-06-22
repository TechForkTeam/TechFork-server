package com.techfork.activity.bookmark.fixture;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.post.domain.Post;
import com.techfork.useraccount.domain.User;

import java.time.LocalDateTime;

public final class BookmarkFixture {

    private BookmarkFixture() {
    }

    public static Bookmark createBookmark(User user, Post post, LocalDateTime bookmarkedAt) {
        return Bookmark.create(user, post, bookmarkedAt);
    }
}
