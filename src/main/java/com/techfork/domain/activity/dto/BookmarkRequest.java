package com.techfork.domain.activity.dto;

import jakarta.validation.constraints.NotNull;

public record BookmarkRequest(
        @NotNull(message = "게시글 ID는 필수입니다.")
        Long postId
) {
}
