package com.techfork.personalization.scheduler;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.personalization.application.PersonalizationProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalizationProfileScheduler {

    private final UserRepository userRepository;
    private final PersonalizationProfileService personalizationProfileService;

    /**
     * 매일 오전 6시(KST)에 최근 24시간 내 활성 사용자의 개인화 프로필을 재생성
     * - 크롤링(5시) 후 1시간 뒤 실행
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void regenerateActiveUserProfiles() {
        log.info("Starting daily personalization profile regeneration for active users");

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        log.info("Found {} active users in the last 24 hours", activeUsers.size());

        activeUsers.forEach(user -> {
            try {
                personalizationProfileService.generatePersonalizationProfile(user.getId());
            } catch (Exception e) {
                log.error("Failed to generate personalization profile for user: {}", user.getId(), e);
            }
        });

        log.info("Completed daily personalization profile regeneration for {} active users", activeUsers.size());
    }
}
