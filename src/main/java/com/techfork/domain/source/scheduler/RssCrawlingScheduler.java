package com.techfork.domain.source.scheduler;

import com.techfork.domain.source.service.CrawlingService;
import com.techfork.global.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * RSS 크롤링 스케줄러
 * - 매일 오전 5시마다 RSS 피드 크롤링 실행
 * - Redis 분산 락으로 중복 실행 방지
 *
 * Note: Job 실행 이력은 Spring Batch의 BATCH_JOB_EXECUTION 테이블에서 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssCrawlingScheduler {

    private final CrawlingService crawlingService;
    private final DistributedLock distributedLock;

    private static final String CRAWLING_LOCK_KEY = "rss-crawling";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30); // 크롤링 최대 실행 시간

    /**
     * 매일 오전 5시마다 RSS 크롤링 실행
     * cron: 0 0 5 * * * -> 매일 오전 5시
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
}
