package com.techfork.post.fixture;

import com.techfork.post.domain.Post;
import com.techfork.post.domain.PostKeyword;

public final class PostKeywordFixture {

    private PostKeywordFixture() {
    }

    public static PostKeyword postKeyword(String keyword, Post post) {
        return PostKeyword.create(keyword, post);
    }
}
