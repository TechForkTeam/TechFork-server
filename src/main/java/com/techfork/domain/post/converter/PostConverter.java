package com.techfork.domain.post.converter;

import com.techfork.domain.post.dto.CompanyListResponse;
import com.techfork.domain.post.dto.PostDetailDto;
import com.techfork.domain.post.dto.PostListResponse;
import com.techfork.domain.post.dto.PostInfoDto;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.source.entity.TechBlog;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostConverter {

    public CompanyListResponse toCompanyListResponse(List<String> companies) {
        return CompanyListResponse.builder()
                .companies(companies)
                .build();
    }

    public PostListResponse toPostListResponse(List<PostInfoDto> postDtos, int requestedSize) {
        boolean hasNext = postDtos.size() > requestedSize;
        List<PostInfoDto> content = hasNext ? postDtos.subList(0, requestedSize) : postDtos;

        Long lastPostId = content.isEmpty() ? null : content.get(content.size() - 1).id();

        return PostListResponse.builder()
                .posts(content)
                .lastPostId(lastPostId)
                .hasNext(hasNext)
                .build();
    }

    public PostDetailDto toDetailDto(Post post) {
        return PostDetailDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .fullContent(post.getFullContent())
                .plainContent(post.getPlainContent())
                .summary(post.getSummary())
                .company(post.getCompany())
                .url(post.getUrl())
                .publishedAt(post.getPublishedAt())
                .crawledAt(post.getCrawledAt())
                .techBlog(toTechBlogInfo(post.getTechBlog()))
                .build();
    }

    private PostDetailDto.TechBlogInfo toTechBlogInfo(TechBlog techBlog) {
        return PostDetailDto.TechBlogInfo.builder()
                .id(techBlog.getId())
                .companyName(techBlog.getCompanyName())
                .blogUrl(techBlog.getBlogUrl())
                .logoUrl(techBlog.getLogoUrl())
                .build();
    }
}
