package com.techfork.domain.activity.service;

import com.techfork.domain.activity.converter.ActivityConverter;
import com.techfork.domain.activity.dto.BookmarkDto;
import com.techfork.domain.activity.dto.BookmarkListResponse;
import com.techfork.domain.activity.dto.ReadPostDto;
import com.techfork.domain.activity.dto.ReadPostListResponse;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.repository.ScrabPostRepository;
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
    private final ReadPostRepository readPostRepository;
    private final ActivityConverter activityConverter;

    public BookmarkListResponse getBookmarks(Long userId, Long lastBookmarkId, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<BookmarkDto> bookmarks = scrabPostRepository.findBookmarksWithCursor(user, lastBookmarkId, pageRequest);
        List<BookmarkDto> bookmarksWithKeywords = attachKeywordsToPostInfoList(bookmarks);

        return activityConverter.toBookmarkListResponse(bookmarksWithKeywords, size);
    }

    public ReadPostListResponse getReadPosts(Long userId, Long lastReadPostId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<ReadPostDto> readPosts = readPostRepository.findReadPostsWithCursor(userId, lastReadPostId, pageRequest);
        List<ReadPostDto> readPostsWithKeywords = attachKeywordsToReadPosts(readPosts);
        List<ReadPostDto> readPostsWithBookmarks = attachBookmarksToReadPosts(readPostsWithKeywords, userId);

        return activityConverter.toReadPostListResponse(readPostsWithBookmarks, size);
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

    private List<ReadPostDto> attachKeywordsToReadPosts(List<ReadPostDto> readPosts) {
        if (readPosts.isEmpty()) {
            return readPosts;
        }

        List<Long> postIds = readPosts.stream()
                .map(ReadPostDto::postId)
                .toList();

        Map<Long, List<String>> keywordMap = postKeywordRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getPost().getId(),
                        Collectors.mapping(PostKeyword::getKeyword, Collectors.toList())
                ));

        return readPosts.stream()
                .map(readPost -> ReadPostDto.builder()
                        .readPostId(readPost.readPostId())
                        .postId(readPost.postId())
                        .title(readPost.title())
                        .shortSummary(readPost.shortSummary())
                        .url(readPost.url())
                        .companyName(readPost.companyName())
                        .logoUrl(readPost.logoUrl())
                        .publishedAt(readPost.publishedAt())
                        .thumbnailUrl(readPost.thumbnailUrl())
                        .viewCount(readPost.viewCount())
                        .keywords(keywordMap.getOrDefault(readPost.postId(), List.of()))
                        .isBookmarked(null)
                        .readAt(readPost.readAt())
                        .build())
                .toList();
    }

    private List<ReadPostDto> attachBookmarksToReadPosts(List<ReadPostDto> readPosts, Long userId) {
        if (readPosts.isEmpty()) {
            return readPosts;
        }

        List<Long> postIds = readPosts.stream()
                .map(ReadPostDto::postId)
                .toList();

        List<Long> bookmarkedPostIds = scrabPostRepository.findBookmarkedPostIds(userId, postIds);

        return readPosts.stream()
                .map(readPost -> ReadPostDto.builder()
                        .readPostId(readPost.readPostId())
                        .postId(readPost.postId())
                        .title(readPost.title())
                        .shortSummary(readPost.shortSummary())
                        .url(readPost.url())
                        .companyName(readPost.companyName())
                        .logoUrl(readPost.logoUrl())
                        .publishedAt(readPost.publishedAt())
                        .thumbnailUrl(readPost.thumbnailUrl())
                        .viewCount(readPost.viewCount())
                        .keywords(readPost.keywords())
                        .isBookmarked(bookmarkedPostIds.contains(readPost.postId()))
                        .readAt(readPost.readAt())
                        .build())
                .toList();
    }
}
