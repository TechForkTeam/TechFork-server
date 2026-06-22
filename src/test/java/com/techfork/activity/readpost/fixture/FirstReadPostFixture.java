package com.techfork.activity.readpost.fixture;

import com.techfork.activity.readpost.domain.FirstReadPost;
import com.techfork.post.domain.Post;
import com.techfork.useraccount.domain.User;

import java.time.LocalDateTime;

public final class FirstReadPostFixture {

    private FirstReadPostFixture() {
    }

    public static FirstReadPost createFirstReadPost(User user, Post post, LocalDateTime firstReadAt) {
        return FirstReadPost.create(user, post, firstReadAt);
    }
}
