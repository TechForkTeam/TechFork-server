package com.techfork.post.application.query;

import com.techfork.post.application.query.composition.PostReadModelCompositionService;
import com.techfork.post.application.query.result.CompanyListItemResult;
import com.techfork.post.application.query.result.GetCompanyListResult;
import com.techfork.post.application.query.result.GetPostDetailResult;
import com.techfork.post.application.query.result.GetPostListResult;
import com.techfork.post.application.query.result.PostListItemResult;
import com.techfork.post.domain.enums.EPostSortType;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.post.infrastructure.row.CompanyRow;
import com.techfork.post.infrastructure.row.PostDetailRow;
import com.techfork.post.infrastructure.row.PostInfoRow;
import com.techfork.global.exception.CommonErrorCode;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostReadModelCompositionService postReadModelCompositionService;

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
        List<PostInfoRow> composedPosts = postReadModelCompositionService.composePostInfoRows(posts, query.userId());
        return toGetPostListResult(composedPosts, query.size());
    }

    public GetPostListResult getPostsByCompanyV2(GetPostsByCompanyV2Query query) {
        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<PostInfoRow> posts = postRepository.findByCompanyNamesWithCursor(query.companies(), query.lastPublishedAt(), query.lastPostId(), pageRequest);
        List<PostInfoRow> composedPosts = postReadModelCompositionService.composePostInfoRows(posts, query.userId());
        return toGetPostListResult(composedPosts, query.size());
    }

    public GetPostListResult getRecentPosts(GetRecentPostsQuery query) {
        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<PostInfoRow> posts;

        if (query.sortBy() == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursor(query.lastPostId(), pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursor(query.lastPostId(), pageRequest);
        }

        List<PostInfoRow> composedPosts = postReadModelCompositionService.composePostInfoRows(posts, query.userId());
        return toGetPostListResult(composedPosts, query.size());
    }

    public GetPostListResult getRecentPostsV2(GetRecentPostsV2Query query) {
        PageRequest pageRequest = PageRequest.of(0, query.size() + 1);
        List<PostInfoRow> posts;

        if (query.sortBy() == EPostSortType.POPULAR) {
            posts = postRepository.findPopularPostsWithCursorV2(query.lastViewCount(), query.lastPostId(), pageRequest);
        } else {
            posts = postRepository.findRecentPostsWithCursorV2(query.lastPublishedAt(), query.lastPostId(), pageRequest);
        }

        List<PostInfoRow> composedPosts = postReadModelCompositionService.composePostInfoRows(posts, query.userId());
        return toGetPostListResult(composedPosts, query.size());
    }

    public GetPostDetailResult getPostDetail(GetPostDetailQuery query) {
        PostDetailRow postDetail = postRepository.findByIdWithTechBlog(query.postId())
                .orElseThrow(() -> new GeneralException(CommonErrorCode.NOT_FOUND));

        return postReadModelCompositionService.composePostDetail(postDetail, query.userId());
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
