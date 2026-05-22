package com.techfork.post.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(name = "PostListResponse")
public record PostListResponse(
        List<PostInfoDto> posts,
        Long lastPostId,
        Long lastViewCount,
        LocalDateTime lastPublishedAt,
        boolean hasNext
) {
}
