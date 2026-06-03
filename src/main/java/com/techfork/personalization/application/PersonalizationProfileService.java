package com.techfork.personalization.application;

import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.useraccount.domain.User;
import com.techfork.personalization.application.generation.PersonalizedProfileGenerator;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizationProfileService {

    private final PersonalizedProfileGenerator personalizedProfileGenerator;
    private final UserLookupService userLookupService;
    private final RecommendationService recommendationService;

    @Async
    @Transactional
    public void generatePersonalizationProfile(Long userId) {
        generatePersonalizationProfileSync(userId);
    }

    /**
     * 개인화 프로필 생성 (동기 버전)
     * 테스트 환경이나 동기 실행이 필요한 경우 사용
     */
    @Transactional
    public void generatePersonalizationProfileSync(Long userId) {
        try {
            personalizedProfileGenerator.generate(userId);

            log.info("Personalization profile generated successfully for userId: {}", userId);

            generateRecommendationsAfterProfile(userId);

        } catch (Exception e) {
            log.error("Failed to generate personalization profile for userId: {}", userId, e);
            throw e;
        }
    }

    /**
     * 개인화 프로필 생성 완료 후 추천 생성
     * 온보딩 또는 관심사 변경 시 새 개인화 프로필 기반으로 추천을 갱신합니다.
     */
    private void generateRecommendationsAfterProfile(Long userId) {
        try {
            User user = userLookupService.getUserOrThrow(userId);

            int recommendationCount = recommendationService.generateRecommendationsForUser(user);

            log.info("Recommendations generated after personalization profile creation for userId: {} - {} recommendations created",
                    userId, recommendationCount);

        } catch (Exception e) {
            log.error("Failed to generate recommendations after personalization profile creation for userId: {}", userId, e);
        }
    }
}
