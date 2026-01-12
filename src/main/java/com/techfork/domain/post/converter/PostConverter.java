package com.techfork.domain.post.converter;

import com.techfork.domain.post.dto.*;
import org.springframework.stereotype.Component;

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
                .companies(companies)
                .build();
    }

    public PostListResponse toPostListResponse(List<PostInfoDto> posts, int requestedSize) {
        boolean hasNext = posts.size() > requestedSize;
        List<PostInfoDto> content = hasNext ? posts.subList(0, requestedSize) : posts;

        Long lastPostId = content.isEmpty() ? null : content.get(content.size() - 1).id();

        return PostListResponse.builder()
                .posts(content)
                .lastPostId(lastPostId)
                .hasNext(hasNext)
                .build();
    }

    public PostDetailDto toPostDetailDto(PostDetailDto baseDto, List<String> keywords) {
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
                .build();
    }
}
