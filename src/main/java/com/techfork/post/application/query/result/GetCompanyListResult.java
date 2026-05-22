package com.techfork.post.application.query.result;

import lombok.Builder;

import java.util.List;

@Builder
public record GetCompanyListResult(
        Integer totalNumber,
        List<CompanyListItemResult> companies
) {
}
