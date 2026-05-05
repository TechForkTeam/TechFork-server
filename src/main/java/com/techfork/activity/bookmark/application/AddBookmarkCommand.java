package com.techfork.activity.bookmark.application;

public record AddBookmarkCommand(
        Long userId,
        Long postId
) {
}
