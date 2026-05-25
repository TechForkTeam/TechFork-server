package com.techfork.useraccount.application.command;

import java.util.List;

public record UpdateUserInterestsCommand(
        Long userId,
        List<UserInterestCommand> interests
) {
}
