package com.techfork.auth.application.command;

public record LogoutCommand(
        String refreshToken
) {
}
