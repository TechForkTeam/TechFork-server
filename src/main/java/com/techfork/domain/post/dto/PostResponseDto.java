package com.techfork.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponseDto {

    @Schema(name = "PostDetailResponse")
    @Builder
    public record Detail(
            Long id,
            String title,
            String summary,
            String company,
            String url,
            String logoUrl,
            LocalDateTime publishedAt,
            Long viewCount,
            List<String> keywords
    ) {
    }

    @Schema(name = "PostInfoDto")
    @Builder
    public record Info(
            Long id,
            String title,
            String company,
            String url,
            String logoUrl,
            LocalDateTime publishedAt,
            Long viewCount,
            List<String> keywords
    ) {
    }

    @Schema(name = "PostListResponse")
    @Builder
    public record PostList(
            List<Info> posts,
            Long lastPostId,
            boolean hasNext
    ) {
    }

    @Schema(name = "CompanyListResponse")
    @Builder
    public record CompanyList(
            List<String> companies
    ) {
    }
}