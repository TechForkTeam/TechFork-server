package com.techfork.domain.recommendation.service;

import com.techfork.useraccount.entity.User;
import com.techfork.useraccount.repository.UserRepository;
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
    private final UserRepository userRepository;

    public void regenerateRecommendations(Long userId) {
        User user = userRepository.getReferenceById(userId);
        int generatedCount = recommendationService.generateRecommendationsForUser(user);
        log.info("사용자 {} 추천 즉시 재생성 완료: {} 개", userId, generatedCount);
    }
}
