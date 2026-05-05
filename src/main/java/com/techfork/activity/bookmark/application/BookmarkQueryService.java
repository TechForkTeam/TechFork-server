package com.techfork.activity.bookmark.application;

import com.techfork.activity.bookmark.presentation.BookmarkConverter;
import com.techfork.activity.bookmark.presentation.BookmarkListResponse;
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
    private final BookmarkConverter bookmarkConverter;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public BookmarkListResponse getBookmarks(GetBookmarksQuery query) {
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<BookmarkDto> bookmarks = bookmarkRepository.findBookmarksWithCursor(user, query.lastBookmarkId(), pageRequest);
        List<BookmarkDto> bookmarksWithKeywords = attachKeywordsToPostInfoList(bookmarks);

        return bookmarkConverter.toBookmarkListResponse(bookmarksWithKeywords, query.size());
    }

    private List<BookmarkDto> attachKeywordsToPostInfoList(List<BookmarkDto> bookmarks) {
        if (bookmarks.isEmpty()) {
            return bookmarks;
        }

        List<Long> postIds = bookmarks.stream()
                .map(BookmarkDto::postId)
                .toList();

        Map<Long, List<String>> keywordMap = postKeywordRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getPost().getId(),
                        Collectors.mapping(PostKeyword::getKeyword, Collectors.toList())
                ));

        return bookmarks.stream()
                .map(post -> BookmarkDto.builder()
                        .bookmarkId(post.bookmarkId())
                        .postId(post.postId())
                        .title(post.title())
                        .shortSummary(post.shortSummary())
                        .url(post.url())
                        .companyName(post.companyName())
                        .logoUrl(post.logoUrl())
                        .publishedAt(post.publishedAt())
                        .thumbnailUrl(thumbnailOptimizer.optimize(post.thumbnailUrl()))
                        .viewCount(post.viewCount())
                        .keywords(keywordMap.getOrDefault(post.postId(), List.of()))
                        .isBookmarked(post.isBookmarked())
                        .build())
                .toList();
    }
}
