package com.techfork.useraccount.application.command;

import java.util.List;

public record CompleteOnboardingCommand(
        Long userId,
        String nickname,
        String email,
        String description,
        List<UserInterestCommand> interests
) {
}
