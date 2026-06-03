package com.techfork.activity.bookmark.application.query.lookup;

import java.util.List;

public record BookmarkPostLookupItem(
        String title,
        List<String> keywords
) {
}
