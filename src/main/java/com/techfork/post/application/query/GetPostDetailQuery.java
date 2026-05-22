package com.techfork.post.application.query;

public record GetPostDetailQuery(
        Long postId,
        Long userId
) {
}
