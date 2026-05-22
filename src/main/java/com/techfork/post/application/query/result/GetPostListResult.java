package com.techfork.post.application.query.result;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record GetPostListResult(
        List<PostListItemResult> posts,
        Long lastPostId,
        Long lastViewCount,
        LocalDateTime lastPublishedAt,
        boolean hasNext
) {
}
