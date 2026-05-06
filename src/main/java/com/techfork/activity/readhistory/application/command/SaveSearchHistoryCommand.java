package com.techfork.activity.readhistory.application.command;

import java.time.LocalDateTime;

public record SaveSearchHistoryCommand(
        Long userId,
        String query,
        LocalDateTime searchedAt
) {
}
