package com.techfork.post.presentation;

import com.techfork.post.application.query.result.CompanyListItemResult;
import com.techfork.post.application.query.result.GetCompanyListResult;
import com.techfork.post.application.query.result.GetPostDetailResult;
import com.techfork.post.application.query.result.GetPostListResult;
import com.techfork.post.application.query.result.PostListItemResult;
import org.springframework.stereotype.Component;

@Component
public class PostConverter {

    public CompanyListResponse toCompanyListResponse(GetCompanyListResult result) {
        return CompanyListResponse.builder()
                .companies(result.companies().stream()
                        .map(CompanyListItemResult::company)
                        .toList())
                .build();
    }

    public CompanyListResponse toCompanyListResponseV2(GetCompanyListResult result) {
        return CompanyListResponse.builder()
                .totalNumber(result.totalNumber())
                .companies(result.companies().stream()
                        .map(company -> CompanyResponse.builder()
                                .company(company.company())
                                .hasNewPost(company.hasNewPost())
                                .logoUrl(company.logoUrl())
                                .build())
                        .toList())
                .build();
    }

    public PostListResponse toPostListResponse(GetPostListResult result) {
        return PostListResponse.builder()
                .posts(result.posts().stream()
                        .map(post -> PostInfoResponse.builder()
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
                .lastPostId(result.lastPostId())
                .lastViewCount(result.lastViewCount())
                .lastPublishedAt(result.lastPublishedAt())
                .hasNext(result.hasNext())
                .build();
    }

    public PostDetailResponse toPostDetailResponse(GetPostDetailResult result) {
        return PostDetailResponse.builder()
                .id(result.id())
                .title(result.title())
                .summary(result.summary())
                .company(result.company())
                .url(result.url())
                .logoUrl(result.logoUrl())
                .publishedAt(result.publishedAt())
                .viewCount(result.viewCount())
                .keywords(result.keywords())
                .isBookmarked(result.isBookmarked())
                .build();
    }
}
