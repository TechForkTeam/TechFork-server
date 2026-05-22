package com.techfork.post.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "CompanyResponse", description = "회사 정보")
public record CompanyResponse(
        String company,
        boolean hasNewPost,
        String logoUrl
) {
}