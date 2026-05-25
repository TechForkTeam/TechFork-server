package com.techfork.useraccount.presentation.response;

import lombok.Builder;

import java.util.List;

@Builder
public record UserInterestResponse(
        List<Interest> interests
) {
    @Builder
    public record Interest(
            String category,
            List<String> keywords
    ) {
    }
}
