package com.techfork.domain.recommendation.service;

import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.useraccount.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RecommendationCommandService {

    private final RecommendationService recommendationService;
    private final UserLookupService userLookupService;

    public void regenerateRecommendations(Long userId) {
        User user = userLookupService.getUserReference(userId);
        int generatedCount = recommendationService.generateRecommendationsForUser(user);
        log.info("사용자 {} 추천 즉시 재생성 완료: {} 개", userId, generatedCount);
    }
}
