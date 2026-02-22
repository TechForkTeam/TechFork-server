package com.techfork.domain.source.listener;

import com.techfork.domain.source.service.WebhookNotificationService;
import com.techfork.global.constant.MdcKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RSS 크롤링 Job 실행 리스너
 * - Job 시작 시: 시작 로깅
 * - Job 종료 시: 성공/실패 처리, 통계 로깅, Webhook 알림
 *
 * Note: Job 실행 이력은 Spring Batch의 BATCH_JOB_EXECUTION 테이블에서 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssCrawlingJobListener implements JobExecutionListener {

    private final WebhookNotificationService webhookNotificationService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        MDC.put(MdcKey.REQUEST_ID, "batch-" + jobExecution.getId());
        MDC.put(MdcKey.USER_ID, "system");
        log.info("RSS crawling job started: jobExecutionId={}, startTime={}",
                jobExecution.getId(), jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        BatchStatus batchStatus = jobExecution.getStatus();

        if (batchStatus == BatchStatus.COMPLETED) {
            handleJobSuccess(jobExecution);
        } else {
            handleJobFailure(jobExecution, "Job failed with status: " + batchStatus);
        }
        MDC.clear();
    }

    /**
     * Job 성공 처리
     */
    private void handleJobSuccess(JobExecution jobExecution) {
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        int readCount = (int) stepExecution.getReadCount();
        int writeCount = (int) stepExecution.getWriteCount();
        int skipCount = (int) stepExecution.getSkipCount();

        long durationMs = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();

        log.info("RSS crawling completed successfully: jobExecutionId={}, total={}, success={}, failed={}, duration={}ms",
                jobExecution.getId(), readCount, writeCount, skipCount, durationMs);
    }

    /**
     * Job 실패 처리 및 Webhook 알림 전송
     */
    private void handleJobFailure(JobExecution jobExecution, String errorMessage) {
        log.error("RSS crawling failed: jobExecutionId={}, error={}",
                jobExecution.getId(), errorMessage);

        // 실패 알림 전송
        Map<String, Object> context = new HashMap<>();
        context.put("errorMessage", errorMessage);
        context.put("timestamp", LocalDateTime.now());
        context.put("jobExecutionId", jobExecution.getId());

        webhookNotificationService.sendCrawlingFailureNotification(context);
    }
}
