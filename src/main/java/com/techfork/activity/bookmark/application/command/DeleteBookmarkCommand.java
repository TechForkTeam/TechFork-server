package com.techfork.activity.bookmark.application.command;

public record DeleteBookmarkCommand(
        Long userId,
        Long postId
) {
}
