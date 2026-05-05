package com.techfork.activity.bookmark.application;

public record DeleteBookmarkCommand(
        Long userId,
        Long postId
) {
}
