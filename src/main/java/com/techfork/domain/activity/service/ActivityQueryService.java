package com.techfork.domain.activity.service;

import com.techfork.domain.activity.converter.ActivityConverter;
import com.techfork.domain.activity.dto.BookmarkDto;
import com.techfork.domain.activity.dto.BookmarkListResponse;
import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.post.dto.PostInfoDto;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.print.Book;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityQueryService {

    private final UserRepository userRepository;
    private final ScrabPostRepository scrabPostRepository;
    private final PostKeywordRepository postKeywordRepository;
    private final ActivityConverter activityConverter;

    public BookmarkListResponse getBookmarks(Long userId, Long lastBookmarkId, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<BookmarkDto> bookmarks = scrabPostRepository.findBookmarksWithCursor(user, lastBookmarkId, pageRequest);
        List<BookmarkDto> bookmarksWithKeywords = attachKeywordsToPostInfoList(bookmarks);

        return activityConverter.toBookmarkListResponse(bookmarksWithKeywords, size);
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
                        .thumbnailUrl(post.thumbnailUrl())
                        .viewCount(post.viewCount())
                        .keywords(keywordMap.getOrDefault(post.postId(), List.of()))
                        .isBookmarked(post.isBookmarked())
                        .build())
                .toList();
    }
}
