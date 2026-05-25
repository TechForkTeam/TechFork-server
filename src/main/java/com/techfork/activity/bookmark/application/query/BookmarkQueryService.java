package com.techfork.activity.bookmark.application.query;

import com.techfork.activity.bookmark.infrastructure.BookmarkQueryRow;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.post.application.query.lookup.PostKeywordLookupService;
import com.techfork.useraccount.entity.User;
import com.techfork.useraccount.service.UserLookupService;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkQueryService {

    private final UserLookupService userLookupService;
    private final BookmarkRepository bookmarkRepository;
    private final PostKeywordLookupService postKeywordLookupService;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public GetBookmarksResult getBookmarks(GetBookmarksQuery query) {
        User user = userLookupService.getUserOrThrow(query.userId());

        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<BookmarkQueryRow> rows = bookmarkRepository.findBookmarksWithCursor(user, query.lastBookmarkId(), pageRequest);
        List<BookmarkItem> bookmarksWithKeywords = attachKeywordsToBookmarks(rows);
        return toGetBookmarksResult(bookmarksWithKeywords, query.size());
    }

    private List<BookmarkItem> attachKeywordsToBookmarks(List<BookmarkQueryRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = rows.stream()
                .map(BookmarkQueryRow::postId)
                .toList();

        Map<Long, List<String>> keywordMap = postKeywordLookupService.getKeywordsByPostIds(postIds);

        return rows.stream()
                .map(row -> BookmarkItem.builder()
                        .bookmarkId(row.bookmarkId())
                        .postId(row.postId())
                        .title(row.title())
                        .shortSummary(row.shortSummary())
                        .url(row.url())
                        .companyName(row.companyName())
                        .logoUrl(row.logoUrl())
                        .publishedAt(row.publishedAt())
                        .thumbnailUrl(thumbnailOptimizer.optimize(row.thumbnailUrl()))
                        .viewCount(row.viewCount())
                        .keywords(keywordMap.getOrDefault(row.postId(), List.of()))
                        .build())
                .toList();
    }

    private GetBookmarksResult toGetBookmarksResult(List<BookmarkItem> bookmarks, int requestedSize) {
        boolean hasNext = bookmarks.size() > requestedSize;
        List<BookmarkItem> content = hasNext ? bookmarks.subList(0, requestedSize) : bookmarks;
        Long lastBookmarkId = content.isEmpty() ? null : content.get(content.size() - 1).bookmarkId();
        return new GetBookmarksResult(content, lastBookmarkId, hasNext);
    }
}
