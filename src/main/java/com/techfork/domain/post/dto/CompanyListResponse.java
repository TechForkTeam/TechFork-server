package com.techfork.domain.post.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CompanyListResponse(
        List<String> companies
) {
}
