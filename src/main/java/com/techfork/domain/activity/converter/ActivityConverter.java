package com.techfork.domain.activity.converter;

import com.techfork.domain.activity.dto.BookmarkDto;
import com.techfork.domain.activity.dto.BookmarkListResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActivityConverter {

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
