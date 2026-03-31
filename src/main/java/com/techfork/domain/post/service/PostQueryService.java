package com.techfork.domain.post.service;

import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.post.converter.PostConverter;
import com.techfork.domain.post.dto.*;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.enums.EPostSortType;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.global.exception.CommonErrorCode;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostKeywordRepository postKeywordRepository;
    private final ScrabPostRepository scrabPostRepository;
    private final PostConverter postConverter;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public CompanyListResponse getCompanies() {
        List<String> companies = postRepository.findDistinctCompanies();
        return postConverter.toCompanyListResponse(companies);
    }

    public CompanyListResponse getCompaniesV2() {
        List<CompanyDto> companies = postRepository.findCompaniesWithDetails();
        return postConverter.toCompanyListResponseV2(companies);
    }

    public PostListResponse getPostsByCompany(String company, Long lastPostId, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoDto> posts = postRepository.findByCompanyWithCursor(company, lastPostId, pageRequest);
        List<PostInfoDto> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (userId != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, userId);
        }

        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostListResponse getPostsByCompanyV2(List<String> companies, LocalDateTime lastPublishedAt, Long lastPostId, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoDto> posts = postRepository.findByCompanyNamesWithCursor(companies, lastPublishedAt, lastPostId, pageRequest);
        List<PostInfoDto> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (userId != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, userId);
        }

        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostListResponse getRecentPosts(EPostSortType sortBy, Long lastPostId, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoDto> posts;

        if (sortBy == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursor(lastPostId, pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursor(lastPostId, pageRequest);
        }

        List<PostInfoDto> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (userId != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, userId);
        }

        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostListResponse getRecentPostsV2(EPostSortType sortBy, Integer lastViewCount, LocalDateTime lastPublishedAt, Long lastPostId, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoDto> posts;

        if (sortBy == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursorV2(lastViewCount, lastPostId, pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursorV2(lastPublishedAt, lastPostId, pageRequest);
        }

        List<PostInfoDto> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (userId != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, userId);
        }

        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostDetailDto getPostDetail(Long postId, Long userId) {
        PostDetailDto postDetail = postRepository.findByIdWithTechBlog(postId)
                .orElseThrow(() -> new GeneralException(CommonErrorCode.NOT_FOUND));

        List<String> keywords = postKeywordRepository.findByPostIdIn(List.of(postId))
                .stream()
                .map(PostKeyword::getKeyword)
                .toList();

        Boolean isBookmarked = null;
        if (userId != null) {
            isBookmarked = !scrabPostRepository.findBookmarkedPostIds(userId, List.of(postId)).isEmpty();
        }

        return postConverter.toPostDetailDto(postDetail, keywords, isBookmarked);
    }

    private List<PostInfoDto> attachKeywordsToPostInfoList(List<PostInfoDto> posts) {
        if (posts.isEmpty()) {
            return posts;
        }

        List<Long> postIds = posts.stream()
                .map(PostInfoDto::id)
                .toList();

        Map<Long, List<String>> keywordMap = postKeywordRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getPost().getId(),
                        Collectors.mapping(PostKeyword::getKeyword, Collectors.toList())
                ));

        return posts.stream()
                .map(post -> PostInfoDto.builder()
                        .id(post.id())
                        .title(post.title())
                        .shortSummary(post.shortSummary())
                        .company(post.company())
                        .url(post.url())
                        .logoUrl(post.logoUrl())
                        .thumbnailUrl(thumbnailOptimizer.optimize(post.thumbnailUrl()))
                        .publishedAt(post.publishedAt())
                        .viewCount(post.viewCount())
                        .keywords(keywordMap.getOrDefault(post.id(), List.of()))
                        .isBookmarked(null)
                        .build())
                .toList();
    }

    private List<PostInfoDto> attachBookmarksToPostInfoList(List<PostInfoDto> posts, Long userId) {
        if (posts.isEmpty()) {
            return posts;
        }

        List<Long> postIds = posts.stream()
                .map(PostInfoDto::id)
                .toList();

        Set<Long> bookmarkedPostIds = scrabPostRepository.findBookmarkedPostIds(userId, postIds)
                .stream()
                .collect(Collectors.toSet());

        return posts.stream()
                .map(post -> PostInfoDto.builder()
                        .id(post.id())
                        .title(post.title())
                        .shortSummary(post.shortSummary())
                        .company(post.company())
                        .url(post.url())
                        .logoUrl(post.logoUrl())
                        .thumbnailUrl(thumbnailOptimizer.optimize(post.thumbnailUrl()))
                        .publishedAt(post.publishedAt())
                        .viewCount(post.viewCount())
                        .keywords(post.keywords())
                        .isBookmarked(bookmarkedPostIds.contains(post.id()))
                        .build())
                .toList();
    }
}
