package com.techfork.useraccount.presentation.request;

import lombok.Builder;

import java.util.List;

@Builder
public record UserInterestRequest(
        String category,
        List<String> keywords
) {
}
