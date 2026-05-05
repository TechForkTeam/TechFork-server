package com.techfork.activity.readpost.service;

import com.techfork.activity.bookmark.repository.BookmarkRepository;
import com.techfork.activity.readpost.converter.ReadPostConverter;
import com.techfork.activity.readpost.dto.ReadPostDto;
import com.techfork.activity.readpost.dto.ReadPostListResponse;
import com.techfork.activity.readpost.repository.ReadPostRepository;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
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
public class ReadPostQueryService {

    private final BookmarkRepository bookmarkRepository;
    private final PostKeywordRepository postKeywordRepository;
    private final ReadPostRepository readPostRepository;
    private final ReadPostConverter readPostConverter;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public ReadPostListResponse getReadPosts(Long userId, Long lastReadPostId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<ReadPostDto> readPosts = readPostRepository.findReadPostsWithCursor(userId, lastReadPostId, pageRequest);
        List<ReadPostDto> readPostsWithKeywords = attachKeywordsToReadPosts(readPosts);
        List<ReadPostDto> readPostsWithBookmarks = attachBookmarksToReadPosts(readPostsWithKeywords, userId);

        return readPostConverter.toReadPostListResponse(readPostsWithBookmarks, size);
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
                        .thumbnailUrl(thumbnailOptimizer.optimize(readPost.thumbnailUrl()))
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

        List<Long> bookmarkedPostIds = bookmarkRepository.findBookmarkedPostIds(userId, postIds);

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
                        .thumbnailUrl(thumbnailOptimizer.optimize(readPost.thumbnailUrl()))
                        .viewCount(readPost.viewCount())
                        .keywords(readPost.keywords())
                        .isBookmarked(bookmarkedPostIds.contains(readPost.postId()))
                        .readAt(readPost.readAt())
                        .build())
                .toList();
    }
}
