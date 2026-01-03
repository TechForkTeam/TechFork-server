package com.techfork.domain.source.listener;

import com.techfork.domain.source.entity.CrawlingHistory;
import com.techfork.domain.source.repository.CrawlingHistoryRepository;
import com.techfork.domain.source.service.WebhookNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RSS 크롤링 Job 실행 리스너
 * - Job 시작 시: CrawlingHistory 생성 및 시작 로깅
 * - Job 종료 시: 성공/실패 처리, 통계 로깅, Webhook 알림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssCrawlingJobListener implements JobExecutionListener {

    private final CrawlingHistoryRepository crawlingHistoryRepository;
    private final WebhookNotificationService webhookNotificationService;

    @Override
    @Transactional
    public void beforeJob(JobExecution jobExecution) {
        Long jobExecutionId = jobExecution.getId();
        log.info("RSS crawling job started: jobExecutionId={}", jobExecutionId);

        CrawlingHistory history = CrawlingHistory.createStarted(jobExecutionId);
        crawlingHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {
        Long jobExecutionId = jobExecution.getId();
        BatchStatus batchStatus = jobExecution.getStatus();

        CrawlingHistory history = crawlingHistoryRepository
                .findByJobExecutionId(jobExecutionId)
                .orElseThrow(() -> new IllegalStateException(
                        "CrawlingHistory not found for jobExecutionId: " + jobExecutionId));

        if (batchStatus == BatchStatus.COMPLETED) {
            handleJobSuccess(history, jobExecution);
        } else {
            handleJobFailure(history, jobExecution, "Job failed with status: " + batchStatus);
        }
    }

    /**
     * Job 성공 처리
     */
    private void handleJobSuccess(CrawlingHistory history, JobExecution jobExecution) {
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        int readCount = (int) stepExecution.getReadCount();
        int writeCount = (int) stepExecution.getWriteCount();
        int skipCount = (int) stepExecution.getSkipCount();

        history.complete(readCount, writeCount, skipCount);
        crawlingHistoryRepository.save(history);

        log.info("RSS crawling completed successfully: " +
                        "total={}, success={}, failed={}",
                readCount, writeCount, skipCount);
    }

    /**
     * Job 실패 처리
     */
    private void handleJobFailure(CrawlingHistory history, JobExecution jobExecution, String errorMessage) {
        history.fail(errorMessage);
        crawlingHistoryRepository.save(history);

        log.error("RSS crawling failed: {}", errorMessage);

        // 실패 알림 전송
        Map<String, Object> context = new HashMap<>();
        context.put("errorMessage", errorMessage);
        context.put("timestamp", LocalDateTime.now());
        context.put("jobExecutionId", jobExecution.getId());

        webhookNotificationService.sendCrawlingFailureNotification(context);
    }
}
