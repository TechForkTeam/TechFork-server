package com.techfork.domain.source.scheduler;

import com.techfork.domain.source.entity.CrawlingHistory;
import com.techfork.domain.source.enums.ECrawlingStatus;
import com.techfork.domain.source.repository.CrawlingHistoryRepository;
import com.techfork.domain.source.service.CrawlingService;
import com.techfork.global.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * RSS 크롤링 스케줄러
 * - 1시간마다 RSS 피드 크롤링 실행
 * - Redis 분산 락으로 중복 실행 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssCrawlingScheduler {

    private final CrawlingService crawlingService;
    private final DistributedLock distributedLock;
    private final CrawlingHistoryRepository crawlingHistoryRepository;

    private static final String CRAWLING_LOCK_KEY = "rss-crawling";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30); // 크롤링 최대 실행 시간

    /**
     * 매일 오전 5시마다 RSS 크롤링 실행
     * cron: 0 0 5 * * * -> 매 시간 정각
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void scheduleCrawling() {
        log.info("RSS crawling scheduler triggered");

        String lockValue = distributedLock.tryLock(CRAWLING_LOCK_KEY, LOCK_TTL);

        if (lockValue == null) {
            log.warn("Another crawling job is already running. Skipping this execution.");
            return;
        }

        try {
            crawlingService.executeCrawling();
        } catch (Exception e) {
            log.error("Unexpected error during scheduled crawling", e);
        } finally {
            distributedLock.unlock(CRAWLING_LOCK_KEY, lockValue);
        }
    }

    /**
     * 5분마다 오래된 RUNNING 상태의 이력을 정리 (좀비 프로세스 방지)
     * cron: 매 5분마다 실행
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void cleanupStaleHistories() {
        log.debug("Checking for stale crawling histories");

        var staleHistories = crawlingHistoryRepository.findByStatusAndStartedAtBefore(
                ECrawlingStatus.RUNNING, java.time.LocalDateTime.now().minusHours(1)
        );

        for (CrawlingHistory history : staleHistories) {
            log.warn("Found stale crawling history: id={}, startedAt={}",
                    history.getId(), history.getStartedAt());
            history.fail("Crawling job timed out or was interrupted");
            crawlingHistoryRepository.save(history);
        }
    }
}
