package com.techfork.domain.recommendation.converter;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.recommendation.dto.RecommendationListResponse;
import com.techfork.domain.recommendation.dto.RecommendedPostDto;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecommendationConverter {

    public RecommendationListResponse toRecommendationListResponse(List<RecommendedPost> recommendedPosts) {
        List<RecommendedPostDto> dtos = recommendedPosts.stream()
                .map(this::toRecommendedPostDto)
                .toList();

        return RecommendationListResponse.builder()
                .recommendations(dtos)
                .totalCount(dtos.size())
                .build();
    }

    public RecommendedPostDto toRecommendedPostDto(RecommendedPost recommendedPost) {
        Post post = recommendedPost.getPost();

        List<String> keywords = post.getKeywords().stream()
                .map(PostKeyword::getKeyword)
                .toList();

        return RecommendedPostDto.builder()
                .id(recommendedPost.getId())
                .postId(post.getId())
                .title(post.getTitle())
                .company(post.getCompany())
                .url(post.getUrl())
                .logoUrl(post.getTechBlog().getLogoUrl())
                .publishedAt(post.getPublishedAt())
                .keywords(keywords)
                .similarityScore(recommendedPost.getSimilarityScore())
                .mmrScore(recommendedPost.getMmrScore())
                .rank(recommendedPost.getRankOrder())
                .recommendedAt(recommendedPost.getRecommendedAt())
                .build();
    }
}
