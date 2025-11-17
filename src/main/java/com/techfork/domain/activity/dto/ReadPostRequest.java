package com.techfork.domain.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "읽은 게시글 저장 요청")
public class ReadPostRequest {

    @Schema(description = "게시글 ID", example = "1")
    @NotNull(message = "게시글 ID는 필수입니다.")
    @Positive(message = "게시글 ID는 양수여야 합니다.")
    private Long postId;

    @Schema(description = "읽은 시간", example = "2024-01-15T10:30:00")
    @NotNull(message = "읽은 시간은 필수입니다.")
    private LocalDateTime readAt;

    @Schema(description = "체류 시간(초)", example = "120")
    private Integer readDurationSeconds;
}
