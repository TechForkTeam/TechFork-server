package com.techfork.domain.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "검색 히스토리 저장 요청")
public class SearchHistoryRequest {

    @Schema(description = "검색어", example = "스프링 부트")
    @NotBlank(message = "검색어는 필수입니다.")
    @Size(max = 200, message = "검색어는 200자 이하여야 합니다.")
    private String searchWord;

    @Schema(description = "검색 시간", example = "2024-01-15T10:30:00")
    @NotNull(message = "검색 시간은 필수입니다.")
    private LocalDateTime searchedAt;
}
