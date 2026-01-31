package com.techfork.domain.source.scheduler;

import com.techfork.domain.source.service.CrawlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RSS 크롤링 스케줄러
 * - 매일 오전 5시마다 RSS 피드 크롤링 실행
 *
 * Note: Job 실행 이력은 Spring Batch의 BATCH_JOB_EXECUTION 테이블에서 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssCrawlingScheduler {

    private final CrawlingService crawlingService;

    /**
     * 매일 오전 5시마다 RSS 크롤링 실행
     * cron: 0 0 5 * * * -> 매일 오전 5시
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void scheduleCrawling() {
        log.info("RSS crawling scheduler triggered");
        crawlingService.executeCrawling();
    }
}
