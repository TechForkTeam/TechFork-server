package com.techfork.domain.useraccount.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserInterestDto(
        String category,
        List<String> keywords
) {
}
