package com.techfork.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(name = "CompanyListResponse")
public record CompanyListResponse(
        List<String> companies
) {
}
