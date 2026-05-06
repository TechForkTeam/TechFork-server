package com.techfork.activity.bookmark.application.command;

public record AddBookmarkCommand(
        Long userId,
        Long postId
) {
}
