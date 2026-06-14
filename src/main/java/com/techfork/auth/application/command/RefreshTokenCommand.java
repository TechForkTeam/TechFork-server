package com.techfork.auth.application.command;

public record RefreshTokenCommand(
        String refreshToken
) {
}
