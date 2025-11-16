package com.techfork.domain.post.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PostListResponse(
        List<PostSummaryDto> posts,
        Long lastPostId,
        boolean hasNext
) {
}
