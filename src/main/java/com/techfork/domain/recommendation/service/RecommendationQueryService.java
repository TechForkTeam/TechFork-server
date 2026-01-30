package com.techfork.domain.recommendation.service;

import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.recommendation.converter.RecommendationConverter;
import com.techfork.domain.recommendation.dto.RecommendationListResponse;
import com.techfork.domain.recommendation.dto.RecommendedPostDto;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationQueryService {

    private final RecommendedPostRepository recommendedPostRepository;
    private final UserRepository userRepository;
    private final RecommendationConverter recommendationConverter;
    private final ScrabPostRepository scrabPostRepository;

    public RecommendationListResponse getRecommendations(Long userId) {
        User user = userRepository.getReferenceById(userId);
        List<RecommendedPost> recommendedPosts = recommendedPostRepository.findByUserOrderByRankAsc(user);
        log.info("사용자 {} 추천 목록 조회: {} 개", userId, recommendedPosts.size());

        RecommendationListResponse response = recommendationConverter.toRecommendationListResponse(recommendedPosts);

        // Attach bookmark status
        response = attachBookmarkStatus(response, userId);

        return response;
    }

    private RecommendationListResponse attachBookmarkStatus(RecommendationListResponse response, Long userId) {
        if (response.recommendations().isEmpty()) {
            return response;
        }

        // Collect postIds
        List<Long> postIds = response.recommendations().stream()
                .map(RecommendedPostDto::postId)
                .collect(Collectors.toList());

        // Fetch bookmark status
        List<Long> bookmarkedPostIds = scrabPostRepository.findBookmarkedPostIds(userId, postIds);

        // Attach bookmark status to recommendations
        List<RecommendedPostDto> updatedRecommendations = response.recommendations().stream()
                .map(dto -> RecommendedPostDto.builder()
                        .id(dto.id())
                        .postId(dto.postId())
                        .title(dto.title())
                        .company(dto.company())
                        .url(dto.url())
                        .logoUrl(dto.logoUrl())
                        .thumbnailUrl(dto.thumbnailUrl())
                        .viewCount(dto.viewCount())
                        .isBookmarked(bookmarkedPostIds.contains(dto.postId()))
                        .publishedAt(dto.publishedAt())
                        .keywords(dto.keywords())
                        .similarityScore(dto.similarityScore())
                        .mmrScore(dto.mmrScore())
                        .rank(dto.rank())
                        .recommendedAt(dto.recommendedAt())
                        .build())
                .collect(Collectors.toList());

        return RecommendationListResponse.builder()
                .recommendations(updatedRecommendations)
                .totalCount(response.totalCount())
                .build();
    }
}
