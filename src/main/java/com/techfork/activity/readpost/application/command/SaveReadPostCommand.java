package com.techfork.activity.readpost.application.command;

import java.time.LocalDateTime;

public record SaveReadPostCommand(
        Long userId,
        Long postId,
        LocalDateTime readAt,
        Integer readDurationSeconds
) {
}
