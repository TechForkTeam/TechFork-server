package com.techfork.useraccount.application.command;

import lombok.Builder;

import java.util.List;

@Builder
public record UserInterestCommand(
        String category,
        List<String> keywords
) {
}
