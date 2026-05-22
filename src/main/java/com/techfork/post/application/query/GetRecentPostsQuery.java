package com.techfork.post.application.query;

import com.techfork.post.domain.enums.EPostSortType;

public record GetRecentPostsQuery(
        EPostSortType sortBy,
        Long lastPostId,
        int size,
        Long userId
) {
}
