package com.techfork.domain.activity.readhistory.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SearchHistoryRequest(
        @JsonAlias("searchWord")
        @NotBlank(message = "검색어는 필수입니다.")
        @Size(max = 200, message = "검색어는 200자 이하여야 합니다.")
        String query,

        @NotNull(message = "검색 시간은 필수입니다.")
        LocalDateTime searchedAt
) {
}
