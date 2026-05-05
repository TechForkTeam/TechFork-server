package com.techfork.domain.activity.bookmark.dto;

import jakarta.validation.constraints.NotNull;

public record BookmarkRequest(
        @NotNull(message = "게시글 ID는 필수입니다.")
        Long postId
) {
}
