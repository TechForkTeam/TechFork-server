package com.techfork.auth.application.command.result;

import lombok.Builder;

@Builder
public record DeveloperTokenResult(
        String developerToken
) {
}
