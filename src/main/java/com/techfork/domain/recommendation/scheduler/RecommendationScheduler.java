package com.techfork.domain.recommendation.scheduler;

import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationScheduler {

    private final UserRepository userRepository;
    private final RecommendationService recommendationService;
    private final RecommendationProperties properties;

    /**
     * 매일 오전 7시(KST)에 활성 사용자 대상 추천 생성
     * - 크롤링(5시) → 프로필 생성(6시) → 추천 생성(7시) 순서
     * - 최근 N시간 이내 활성 사용자만 대상
     * - 향후 추천 알림 기능 추가 예정
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void generateDailyRecommendations() {
        log.info("활성 사용자 대상으로 게시글 추천 시작");

        LocalDateTime since = LocalDateTime.now().minusHours(properties.getActiveUserHours());
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        log.info("{} 명의 활성 사용자를 찾았습니다. ({} 시간 이내)", activeUsers.size(), properties.getActiveUserHours());

        int totalRecommendations = 0;
        int successCount = 0;
        int failCount = 0;

        for (User user : activeUsers) {
            try {
                int count = recommendationService.generateRecommendationsForUser(user);
                totalRecommendations += count;
                successCount++;

                if (count > 0) {
                    log.debug("사용자 {}에게 {} 개 추천 생성 완료", user.getId(), count);
                }
            } catch (Exception e) {
                failCount++;
                log.error("사용자 {} 추천 생성 실패", user.getId(), e);
            }
        }

        log.info("일일 추천 생성 완료: 전체 사용자={}, 성공={}, 실패={}, 총 추천 개수={}",
                activeUsers.size(), successCount, failCount, totalRecommendations);
    }
}
