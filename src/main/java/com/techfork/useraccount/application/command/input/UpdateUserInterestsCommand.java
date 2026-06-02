package com.techfork.useraccount.application.command.input;

import java.util.List;

public record UpdateUserInterestsCommand(
        Long userId,
        List<UserInterestCommand> interests
) {
}
