package com.techfork.domain.post.converter;

import com.techfork.domain.post.dto.CompanyListResponse;
import com.techfork.domain.post.dto.PostListResponse;
import com.techfork.domain.post.dto.PostSummaryDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostConverter {

    public CompanyListResponse toCompanyListResponse(List<String> companies) {
        return CompanyListResponse.builder()
                .companies(companies)
                .build();
    }

    public PostListResponse toPostListResponse(List<PostSummaryDto> postDtos, int requestedSize) {
        boolean hasNext = postDtos.size() > requestedSize;
        List<PostSummaryDto> content = hasNext ? postDtos.subList(0, requestedSize) : postDtos;

        Long lastPostId = content.isEmpty() ? null : content.get(content.size() - 1).id();

        return PostListResponse.builder()
                .posts(content)
                .lastPostId(lastPostId)
                .hasNext(hasNext)
                .build();
    }
}
