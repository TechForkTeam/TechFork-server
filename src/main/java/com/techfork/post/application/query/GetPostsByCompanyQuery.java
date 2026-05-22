package com.techfork.post.application.query;

public record GetPostsByCompanyQuery(
        String company,
        Long lastPostId,
        int size,
        Long userId
) {
}
