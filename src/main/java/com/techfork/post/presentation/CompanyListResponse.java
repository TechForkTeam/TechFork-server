package com.techfork.post.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(name = "CompanyListResponse")
public record CompanyListResponse(
        Integer totalNumber,
        @Schema(description = "회사 목록 (V1: String, V2: CompanyResponse)")
        List<?> companies
) {
}
