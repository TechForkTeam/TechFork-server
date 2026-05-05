package com.techfork.activity.bookmark.presentation;

import com.techfork.activity.bookmark.application.BookmarkDto;
import lombok.Builder;

import java.util.List;

@Builder
public record BookmarkListResponse(
        List<BookmarkDto> bookmarks,
        Long lastBookmarkId,
        boolean hasNext
) {
}
