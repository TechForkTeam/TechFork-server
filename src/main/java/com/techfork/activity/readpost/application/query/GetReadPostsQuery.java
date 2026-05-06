package com.techfork.activity.readpost.application.query;

public record GetReadPostsQuery(
        Long userId,
        Long lastReadPostId,
        int size
) {
}
