package com.techfork.domain.post.converter;

import com.techfork.domain.post.dto.PostResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostConverter {

    public PostResponseDto.CompanyList toCompanyListResponse(List<String> companies) {
        return PostResponseDto.CompanyList.builder()
                .companies(companies)
                .build();
    }

    public PostResponseDto.PostList toPostListResponse(List<PostResponseDto.Info> posts, int requestedSize) {
        boolean hasNext = posts.size() > requestedSize;
        List<PostResponseDto.Info> content = hasNext ? posts.subList(0, requestedSize) : posts;

        Long lastPostId = content.isEmpty() ? null : content.get(content.size() - 1).id();

        return PostResponseDto.PostList.builder()
                .posts(content)
                .lastPostId(lastPostId)
                .hasNext(hasNext)
                .build();
    }

    public PostResponseDto.Detail toPostDetailDto(PostResponseDto.Detail baseDto, List<String> keywords) {
        return PostResponseDto.Detail.builder()
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
