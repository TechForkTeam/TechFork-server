package com.techfork.activity.bookmark.application.query;

public record GetBookmarksQuery(
        Long userId,
        Long lastBookmarkId,
        int size
) {
}
