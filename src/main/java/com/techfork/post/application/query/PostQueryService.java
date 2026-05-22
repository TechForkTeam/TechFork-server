package com.techfork.post.application.query;

import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.post.application.query.result.CompanyListItemResult;
import com.techfork.post.application.query.result.GetCompanyListResult;
import com.techfork.post.application.query.result.GetPostDetailResult;
import com.techfork.post.application.query.result.GetPostListResult;
import com.techfork.post.application.query.result.PostListItemResult;
import com.techfork.post.domain.PostKeyword;
import com.techfork.post.domain.enums.EPostSortType;
import com.techfork.post.infrastructure.PostKeywordRepository;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.post.infrastructure.row.CompanyRow;
import com.techfork.post.infrastructure.row.PostDetailRow;
import com.techfork.post.infrastructure.row.PostInfoRow;
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
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    public GetCompanyListResult getCompanies() {
        List<String> companies = postRepository.findDistinctCompanies();
        return GetCompanyListResult.builder()
                .totalNumber(null)
                .companies(companies.stream()
                        .map(company -> CompanyListItemResult.builder()
                                .company(company)
                                .build())
                        .toList())
                .build();
    }

    public GetCompanyListResult getCompaniesV2() {
        List<CompanyRow> companies = postRepository.findCompaniesWithDetails();
        return GetCompanyListResult.builder()
                .totalNumber(companies.size())
                .companies(companies.stream()
                        .map(company -> CompanyListItemResult.builder()
                                .company(company.company())
                                .hasNewPost(company.hasNewPost())
                                .logoUrl(company.logoUrl())
                                .build())
                        .toList())
                .build();
    }

    public GetPostListResult getPostsByCompany(GetPostsByCompanyQuery query) {
        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<PostInfoRow> posts = postRepository.findByCompanyWithCursor(query.company(), query.lastPostId(), pageRequest);
        List<PostInfoRow> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (query.userId() != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, query.userId());
        }

        return toGetPostListResult(postsWithKeywords, query.size());
    }

    public GetPostListResult getPostsByCompanyV2(GetPostsByCompanyV2Query query) {
        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<PostInfoRow> posts = postRepository.findByCompanyNamesWithCursor(query.companies(), query.lastPublishedAt(), query.lastPostId(), pageRequest);
        List<PostInfoRow> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (query.userId() != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, query.userId());
        }

        return toGetPostListResult(postsWithKeywords, query.size());
    }

    public GetPostListResult getRecentPosts(GetRecentPostsQuery query) {
        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<PostInfoRow> posts;

        if (query.sortBy() == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursor(query.lastPostId(), pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursor(query.lastPostId(), pageRequest);
        }

        List<PostInfoRow> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (query.userId() != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, query.userId());
        }

        return toGetPostListResult(postsWithKeywords, query.size());
    }

    public GetPostListResult getRecentPostsV2(GetRecentPostsV2Query query) {
        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<PostInfoRow> posts;

        if (query.sortBy() == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursorV2(query.lastViewCount(), query.lastPostId(), pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursorV2(query.lastPublishedAt(), query.lastPostId(), pageRequest);
        }

        List<PostInfoRow> postsWithKeywords = attachKeywordsToPostInfoList(posts);

        if (query.userId() != null) {
            postsWithKeywords = attachBookmarksToPostInfoList(postsWithKeywords, query.userId());
        }

        return toGetPostListResult(postsWithKeywords, query.size());
    }

    public GetPostDetailResult getPostDetail(GetPostDetailQuery query) {
        PostDetailRow postDetail = postRepository.findByIdWithTechBlog(query.postId())
                .orElseThrow(() -> new GeneralException(CommonErrorCode.NOT_FOUND));

        List<String> keywords = postKeywordRepository.findByPostIdIn(List.of(query.postId()))
                .stream()
                .map(PostKeyword::getKeyword)
                .toList();

        Boolean isBookmarked = null;
        if (query.userId() != null) {
            isBookmarked = !bookmarkRepository.findBookmarkedPostIds(query.userId(), List.of(query.postId())).isEmpty();
        }

        return GetPostDetailResult.builder()
                .id(postDetail.id())
                .title(postDetail.title())
                .summary(postDetail.summary())
                .company(postDetail.company())
                .url(postDetail.url())
                .logoUrl(postDetail.logoUrl())
                .publishedAt(postDetail.publishedAt())
                .viewCount(postDetail.viewCount())
                .keywords(keywords)
                .isBookmarked(isBookmarked)
                .build();
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

    private GetPostListResult toGetPostListResult(List<PostInfoRow> posts, int requestedSize) {
        boolean hasNext = posts.size() > requestedSize;
        List<PostInfoRow> content = hasNext ? posts.subList(0, requestedSize) : posts;

        Long lastPostId = null;
        Long lastViewCount = null;
        LocalDateTime lastPublishedAt = null;

        if (!content.isEmpty()) {
            PostInfoRow lastPost = content.get(content.size() - 1);
            lastPostId = lastPost.id();
            lastViewCount = lastPost.viewCount();
            lastPublishedAt = lastPost.publishedAt();
        }

        return GetPostListResult.builder()
                .posts(content.stream()
                        .map(post -> PostListItemResult.builder()
                                .id(post.id())
                                .title(post.title())
                                .shortSummary(post.shortSummary())
                                .company(post.company())
                                .url(post.url())
                                .logoUrl(post.logoUrl())
                                .thumbnailUrl(post.thumbnailUrl())
                                .publishedAt(post.publishedAt())
                                .viewCount(post.viewCount())
                                .keywords(post.keywords())
                                .isBookmarked(post.isBookmarked())
                                .build())
                        .toList())
                .lastPostId(lastPostId)
                .lastViewCount(lastViewCount)
                .lastPublishedAt(lastPublishedAt)
                .hasNext(hasNext)
                .build();
    }
}
