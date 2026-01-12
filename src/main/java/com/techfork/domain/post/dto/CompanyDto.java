package com.techfork.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(name = "CompanyDto", description = "회사 정보")
public record CompanyDto(
        String company,
        boolean hasNewPost,
        String logoUrl
) {
}