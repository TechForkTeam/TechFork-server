package com.techfork.domain.recommendation.service;

import com.techfork.domain.activity.bookmark.repository.BookmarkRepository;
import com.techfork.domain.recommendation.converter.RecommendationConverter;
import com.techfork.domain.recommendation.dto.RecommendationListResponse;
import com.techfork.domain.recommendation.dto.RecommendedPostDto;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.repository.UserRepository;
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
    private final BookmarkRepository bookmarkRepository;

    public RecommendationListResponse getRecommendations(Long userId) {
        User user = userRepository.getReferenceById(userId);
        List<RecommendedPost> recommendedPosts = recommendedPostRepository.findByUserOrderByRankAsc(user);
        log.info("사용자 {} 추천 목록 조회: {} 개", userId, recommendedPosts.size());

        RecommendationListResponse response = recommendationConverter.toRecommendationListResponse(recommendedPosts);
        response = attachBookmarkStatus(response, userId);

        return response;
    }

    private RecommendationListResponse attachBookmarkStatus(RecommendationListResponse response, Long userId) {
        if (response.recommendations().isEmpty()) {
            return response;
        }

        List<Long> postIds = response.recommendations().stream()
                .map(RecommendedPostDto::postId)
                .toList();
        List<Long> bookmarkedPostIds = bookmarkRepository.findBookmarkedPostIds(userId, postIds);

        List<RecommendedPostDto> updatedRecommendations = response.recommendations().stream()
                .map(dto -> dto.withBookmarkStatus(bookmarkedPostIds.contains(dto.postId())))
                .toList();

        return RecommendationListResponse.builder()
                .recommendations(updatedRecommendations)
                .totalCount(response.totalCount())
                .build();
    }
}
