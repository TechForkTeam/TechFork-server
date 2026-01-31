package com.techfork.domain.user.scheduler;

import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.domain.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileScheduler {

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;

    /**
     * 매일 오전 6시(KST)에 최근 24시간 내 활성 사용자의 프로필을 재생성
     * - 크롤링(5시) 후 1시간 뒤 실행
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void regenerateActiveUserProfiles() {
        log.info("Starting daily user profile regeneration for active users");

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        log.info("Found {} active users in the last 24 hours", activeUsers.size());

        activeUsers.forEach(user -> {
            try {
                userProfileService.generateUserProfile(user.getId());
            } catch (Exception e) {
                log.error("Failed to generate profile for user: {}", user.getId(), e);
            }
        });

        log.info("Completed daily user profile regeneration for {} active users", activeUsers.size());
    }
}
