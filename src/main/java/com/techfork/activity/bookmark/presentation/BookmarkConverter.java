package com.techfork.activity.bookmark.presentation;

import com.techfork.activity.bookmark.application.query.BookmarkItem;
import com.techfork.activity.bookmark.application.query.GetBookmarksResult;
import org.springframework.stereotype.Component;

@Component
public class BookmarkConverter {

    public BookmarkListResponse toBookmarkListResponse(GetBookmarksResult result) {
        return BookmarkListResponse.builder()
                .bookmarks(result.bookmarks().stream()
                        .map(this::toBookmarkResponse)
                        .toList())
                .lastBookmarkId(result.lastBookmarkId())
                .hasNext(result.hasNext())
                .build();
    }

    private BookmarkListResponse.Item toBookmarkResponse(BookmarkItem bookmark) {
        return BookmarkListResponse.Item.builder()
                .bookmarkId(bookmark.bookmarkId())
                .postId(bookmark.postId())
                .title(bookmark.title())
                .shortSummary(bookmark.shortSummary())
                .url(bookmark.url())
                .companyName(bookmark.companyName())
                .logoUrl(bookmark.logoUrl())
                .publishedAt(bookmark.publishedAt())
                .thumbnailUrl(bookmark.thumbnailUrl())
                .viewCount(bookmark.viewCount())
                .keywords(bookmark.keywords())
                .isBookmarked(bookmark.isBookmarked())
                .build();
    }
}
