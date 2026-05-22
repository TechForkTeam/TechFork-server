package com.techfork.post.application.query;

import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.post.domain.PostKeyword;
import com.techfork.post.domain.enums.EPostSortType;
import com.techfork.post.infrastructure.PostKeywordRepository;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.post.infrastructure.row.CompanyRow;
import com.techfork.post.infrastructure.row.PostDetailRow;
import com.techfork.post.infrastructure.row.PostInfoRow;
import com.techfork.post.presentation.CompanyListResponse;
import com.techfork.post.presentation.PostDetailResponse;
import com.techfork.post.presentation.PostListResponse;
import com.techfork.post.presentation.PostConverter;
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
    private final BookmarkRepository bookmarkRepository;
    private final PostConverter postConverter;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public CompanyListResponse getCompanies() {
        List<String> companies = postRepository.findDistinctCompanies();
        return postConverter.toCompanyListResponse(companies);
    }

    public CompanyListResponse getCompaniesV2() {
        List<CompanyRow> companies = postRepository.findCompaniesWithDetails();
        return postConverter.toCompanyListResponseV2(companies);
    }

    public PostListResponse getPostsByCompany(String company, Long lastPostId, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoRow> posts = postRepository.findByCompanyWithCursor(company, lastPostId, pageRequest);
        List<PostInfoRow> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (userId != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, userId);
        }

        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostListResponse getPostsByCompanyV2(List<String> companies, LocalDateTime lastPublishedAt, Long lastPostId, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoRow> posts = postRepository.findByCompanyNamesWithCursor(companies, lastPublishedAt, lastPostId, pageRequest);
        List<PostInfoRow> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (userId != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, userId);
        }

        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostListResponse getRecentPosts(EPostSortType sortBy, Long lastPostId, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoRow> posts;

        if (sortBy == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursor(lastPostId, pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursor(lastPostId, pageRequest);
        }

        List<PostInfoRow> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (userId != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, userId);
        }

        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostListResponse getRecentPostsV2(EPostSortType sortBy, Integer lastViewCount, LocalDateTime lastPublishedAt, Long lastPostId, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<PostInfoRow> posts;

        if (sortBy == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursorV2(lastViewCount, lastPostId, pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursorV2(lastPublishedAt, lastPostId, pageRequest);
        }

        List<PostInfoRow> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (userId != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, userId);
        }

        return postConverter.toPostListResponse(postsWithKeywords, size);
    }

    public PostDetailResponse getPostDetail(Long postId, Long userId) {
        PostDetailRow postDetail = postRepository.findByIdWithTechBlog(postId)
                .orElseThrow(() -> new GeneralException(CommonErrorCode.NOT_FOUND));

        List<String> keywords = postKeywordRepository.findByPostIdIn(List.of(postId))
                .stream()
                .map(PostKeyword::getKeyword)
                .toList();

        Boolean isBookmarked = null;
        if (userId != null) {
            isBookmarked = !bookmarkRepository.findBookmarkedPostIds(userId, List.of(postId)).isEmpty();
        }

        return postConverter.toPostDetailResponse(postDetail, keywords, isBookmarked);
    }

    private List<PostInfoRow> attachKeywordsToPostInfoList(List<PostInfoRow> posts) {
        if (posts.isEmpty()) {
            return posts;
        }

        List<Long> postIds = posts.stream()
                .map(PostInfoRow::id)
                .toList();

        Map<Long, List<String>> keywordMap = postKeywordRepository.findByPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getPost().getId(),
                        Collectors.mapping(PostKeyword::getKeyword, Collectors.toList())
                ));

        return posts.stream()
                .map(post -> PostInfoRow.builder()
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

    private List<PostInfoRow> attachBookmarksToPostInfoList(List<PostInfoRow> posts, Long userId) {
        if (posts.isEmpty()) {
            return posts;
        }

        List<Long> postIds = posts.stream()
                .map(PostInfoRow::id)
                .toList();

        Set<Long> bookmarkedPostIds = bookmarkRepository.findBookmarkedPostIds(userId, postIds)
                .stream()
                .collect(Collectors.toSet());

        return posts.stream()
                .map(post -> PostInfoRow.builder()
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
