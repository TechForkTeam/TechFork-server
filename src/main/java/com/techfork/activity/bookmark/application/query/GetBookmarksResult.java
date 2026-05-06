package com.techfork.activity.bookmark.application.query;

import java.util.List;

public record GetBookmarksResult(
        List<BookmarkItem> bookmarks,
        Long lastBookmarkId,
        boolean hasNext
) {
}
