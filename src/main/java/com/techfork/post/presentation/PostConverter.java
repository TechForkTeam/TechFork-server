package com.techfork.post.presentation;

import com.techfork.post.application.dto.*;
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

    public CompanyListResponse toCompanyListResponseV2(List<CompanyDto> companies) {
        return CompanyListResponse.builder()
                .totalNumber(companies.size())
                .companies(companies)
                .build();
    }

    public PostListResponse toPostListResponse(List<PostInfoDto> posts, int requestedSize) {
        boolean hasNext = posts.size() > requestedSize;
        List<PostInfoDto> content = hasNext ? posts.subList(0, requestedSize) : posts;

        Long lastPostId = null;
        Long lastViewCount = null;
        LocalDateTime lastPublishedAt = null;

        if (!content.isEmpty()) {
            PostInfoDto lastPost = content.get(content.size() - 1);
            lastPostId = lastPost.id();
            lastViewCount = lastPost.viewCount();
            lastPublishedAt = lastPost.publishedAt();
        }

        return PostListResponse.builder()
                .posts(content)
                .lastPostId(lastPostId)
                .lastViewCount(lastViewCount)
                .lastPublishedAt(lastPublishedAt)
                .hasNext(hasNext)
                .build();
    }

    public PostDetailDto toPostDetailDto(PostDetailDto baseDto, List<String> keywords, Boolean isBookmarked) {
        return PostDetailDto.builder()
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
