package com.techfork.activity.readpost.fixture;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.post.domain.Post;
import com.techfork.useraccount.domain.User;

import java.time.LocalDateTime;

public final class ReadPostFixture {

    private ReadPostFixture() {
    }

    public static ReadPost createReadPost(User user, Post post, LocalDateTime readAt, Integer readDurationSeconds) {
        return ReadPost.create(user, post, readAt, readDurationSeconds);
    }
}
