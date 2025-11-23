package com.techfork.domain.recommendation.service;

import com.techfork.domain.recommendation.converter.RecommendationConverter;
import com.techfork.domain.recommendation.dto.RecommendationListResponse;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationQueryService {

    private final RecommendedPostRepository recommendedPostRepository;
    private final UserRepository userRepository;
    private final RecommendationConverter recommendationConverter;

    public RecommendationListResponse getRecommendations(Long userId) {
        User user = userRepository.getReferenceById(userId);
        List<RecommendedPost> recommendedPosts = recommendedPostRepository.findByUserOrderByRankAsc(user);
        log.info("사용자 {} 추천 목록 조회: {} 개", userId, recommendedPosts.size());
        return recommendationConverter.toRecommendationListResponse(recommendedPosts);
    }
}
