package com.techfork.domain.activity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ReadPostRequest(
        @NotNull(message = "게시글 ID는 필수입니다.")
        @Positive(message = "게시글 ID는 양수여야 합니다.")
        Long postId,

        @NotNull(message = "읽은 시간은 필수입니다.")
        LocalDateTime readAt,

        Integer readDurationSeconds
) {
}
