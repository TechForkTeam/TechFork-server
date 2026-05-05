package com.techfork.activity.bookmark.application.query;

import com.techfork.activity.bookmark.infrastructure.BookmarkQueryRow;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PostKeywordRepository postKeywordRepository;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public GetBookmarksResult getBookmarks(GetBookmarksQuery query) {
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

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

        Map<Long, List<String>> keywordMap = postKeywordRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getPost().getId(),
                        Collectors.mapping(PostKeyword::getKeyword, Collectors.toList())
                ));

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
