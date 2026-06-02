package com.techfork.useraccount.application.command.input;

public record UpdateAccountProfileCommand(
        Long userId,
        String nickName,
        String description
) {
}
