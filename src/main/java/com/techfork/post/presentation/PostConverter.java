package com.techfork.post.presentation;

import com.techfork.post.infrastructure.row.CompanyRow;
import com.techfork.post.infrastructure.row.PostDetailRow;
import com.techfork.post.infrastructure.row.PostInfoRow;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PostConverter {

    public CompanyListResponse toCompanyListResponse(List<String> companies) {
        return CompanyListResponse.builder()
                .companies(companies)
                .build();
    }

    public CompanyListResponse toCompanyListResponseV2(List<CompanyRow> companies) {
        return CompanyListResponse.builder()
                .totalNumber(companies.size())
                .companies(companies.stream()
                        .map(company -> CompanyResponse.builder()
                                .company(company.company())
                                .hasNewPost(company.hasNewPost())
                                .logoUrl(company.logoUrl())
                                .build())
                        .toList())
                .build();
    }

    public PostListResponse toPostListResponse(List<PostInfoRow> posts, int requestedSize) {
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

        return PostListResponse.builder()
                .posts(content.stream()
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
                .lastPostId(lastPostId)
                .lastViewCount(lastViewCount)
                .lastPublishedAt(lastPublishedAt)
                .hasNext(hasNext)
                .build();
    }

    public PostDetailResponse toPostDetailResponse(PostDetailRow baseDto, List<String> keywords, Boolean isBookmarked) {
        return PostDetailResponse.builder()
                .id(baseDto.id())
                .title(baseDto.title())
                .summary(baseDto.summary())
                .company(baseDto.company())
                .url(baseDto.url())
                .logoUrl(baseDto.logoUrl())
                .publishedAt(baseDto.publishedAt())
                .viewCount(baseDto.viewCount())
                .keywords(keywords)
                .isBookmarked(isBookmarked)
                .build();
    }
}
