package com.techfork.post.application.query;

import com.techfork.post.domain.enums.EPostSortType;

import java.time.LocalDateTime;

public record GetRecentPostsV2Query(
        EPostSortType sortBy,
        Integer lastViewCount,
        LocalDateTime lastPublishedAt,
        Long lastPostId,
        int size,
        Long userId
) {
}
