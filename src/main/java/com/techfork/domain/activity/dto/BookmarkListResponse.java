package com.techfork.domain.activity.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record BookmarkListResponse(
        List<BookmarkDto> bookmarks,
        Long lastBookmarkId,
        boolean hasNext
) {
}
