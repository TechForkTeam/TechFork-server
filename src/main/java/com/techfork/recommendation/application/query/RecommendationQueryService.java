package com.techfork.recommendation.application.query;

import com.techfork.activity.bookmark.application.query.lookup.BookmarkLookupService;
import com.techfork.domain.recommendation.converter.RecommendationConverter;
import com.techfork.domain.recommendation.dto.RecommendationListResponse;
import com.techfork.domain.recommendation.dto.RecommendedPostDto;
import com.techfork.recommendation.domain.RecommendedPost;
import com.techfork.recommendation.infrastructure.RecommendedPostRepository;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.useraccount.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationQueryService {

    private final RecommendedPostRepository recommendedPostRepository;
    private final UserLookupService userLookupService;
    private final RecommendationConverter recommendationConverter;
    private final BookmarkLookupService bookmarkLookupService;

    public RecommendationListResponse getRecommendations(Long userId) {
        User user = userLookupService.getUserReference(userId);
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
        Set<Long> bookmarkedPostIds = bookmarkLookupService.getBookmarkedPostIds(userId, postIds);

        List<RecommendedPostDto> updatedRecommendations = response.recommendations().stream()
                .map(dto -> dto.withBookmarkStatus(bookmarkedPostIds.contains(dto.postId())))
                .toList();

        return RecommendationListResponse.builder()
                .recommendations(updatedRecommendations)
                .totalCount(response.totalCount())
                .build();
    }
}
