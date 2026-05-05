package com.techfork.activity.bookmark.converter;

import com.techfork.activity.bookmark.dto.BookmarkDto;
import com.techfork.activity.bookmark.dto.BookmarkListResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookmarkConverter {

    public BookmarkListResponse toBookmarkListResponse(List<BookmarkDto> bookmarks, int requestedSize) {
        boolean hasNext = bookmarks.size() > requestedSize;
        List<BookmarkDto> content = hasNext ? bookmarks.subList(0, requestedSize) : bookmarks;

        Long lastBookmarkId = content.isEmpty() ? null : content.get(content.size() - 1).bookmarkId();

        return BookmarkListResponse.builder()
                .bookmarks(content)
                .lastBookmarkId(lastBookmarkId)
                .hasNext(hasNext)
                .build();
    }
}
