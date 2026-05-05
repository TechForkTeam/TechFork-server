package com.techfork.activity.bookmark.application;

public record GetBookmarksQuery(
        Long userId,
        Long lastBookmarkId,
        int size
) {
}
