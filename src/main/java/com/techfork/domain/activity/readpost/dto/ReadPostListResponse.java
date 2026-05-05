package com.techfork.domain.activity.readpost.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ReadPostListResponse(
        List<ReadPostDto> readPosts,
        Long lastReadPostId,
        boolean hasNext
) {
}
