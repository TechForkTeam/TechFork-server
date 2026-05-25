package com.techfork.useraccount.application.query.result;

import lombok.Builder;

import java.util.List;

@Builder
public record UserInterestResult(
        String category,
        List<String> keywords
) {
}
