package com.techfork.useraccount.application.command;

public record UpdateAccountProfileCommand(
        Long userId,
        String nickName,
        String description
) {
}
