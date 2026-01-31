package com.techfork.domain.source.scheduler;

import com.techfork.domain.source.service.CrawlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssCrawlingScheduler {

    private final CrawlingService crawlingService;

    /**
     * 매일 오전 5시(KST)마다 RSS 크롤링 실행
     * - 새 글 수집 후 프로필/추천 생성 파이프라인 시작
     * cron: 0 0 5 * * * -> 매일 오전 5시
     */
    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void scheduleCrawling() {
        log.info("RSS crawling scheduler triggered");
        crawlingService.executeCrawling();
    }
}
