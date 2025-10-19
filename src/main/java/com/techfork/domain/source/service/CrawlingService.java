package com.techfork.domain.source.service;

import com.techfork.domain.source.entity.CrawlingHistory;
import com.techfork.domain.source.repository.CrawlingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RSS 크롤링 실행 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    private final JobLauncher jobLauncher;
    private final Job rssCrawlingJob;
    private final CrawlingHistoryRepository crawlingHistoryRepository;
    private final WebhookNotificationService webhookNotificationService;

    public void executeCrawling() {
        log.info("Starting RSS crawling job");

        CrawlingHistory history = null;
        JobExecution jobExecution = null;

        try {
            // Job 파라미터 생성 (매번 다른 파라미터로 실행)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobExecution = jobLauncher.run(rssCrawlingJob, jobParameters);
            Long jobExecutionId = jobExecution.getId();

            history = CrawlingHistory.createStarted(jobExecutionId);
            crawlingHistoryRepository.save(history);

            log.info("RSS crawling job started: jobExecutionId={}", jobExecutionId);

            // Job 실행 완료 대기 및 결과 처리
            waitForJobCompletion(jobExecution, history);

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("Job is already running", e);
            if (history != null) {
                history.fail("Job is already running");
                crawlingHistoryRepository.save(history);
            }
        } catch (JobRestartException e) {
            log.error("Job restart failed", e);
            handleJobFailure(history, jobExecution, "Job restart failed: " + e.getMessage());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("Job instance already complete", e);
            handleJobFailure(history, jobExecution, "Job instance already complete: " + e.getMessage());
        } catch (JobParametersInvalidException e) {
            log.error("Invalid job parameters", e);
            handleJobFailure(history, jobExecution, "Invalid job parameters: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during crawling", e);
            handleJobFailure(history, jobExecution, "Unexpected error: " + e.getMessage());
        }
    }

    private void waitForJobCompletion(JobExecution jobExecution, CrawlingHistory history) {
        BatchStatus batchStatus = jobExecution.getStatus();

        while (batchStatus.isRunning()) {
            try {
                Thread.sleep(1000);
                batchStatus = jobExecution.getStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Job execution monitoring interrupted", e);
                break;
            }
        }

        // Job 실행 결과 처리
        if (batchStatus == BatchStatus.COMPLETED) {
            handleJobSuccess(history, jobExecution);
        } else {
            handleJobFailure(history, jobExecution,
                    "Job failed with status: " + batchStatus);
        }
    }

    /**
     * Job 성공 처리
     */
    @Transactional
    public void handleJobSuccess(CrawlingHistory history, JobExecution jobExecution) {
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
    @Transactional
    public void handleJobFailure(CrawlingHistory history, JobExecution jobExecution, String errorMessage) {
        if (history != null) {
            history.fail(errorMessage);
            crawlingHistoryRepository.save(history);
        }

        log.error("RSS crawling failed: {}", errorMessage);

        // 실패 알림 전송
        Map<String, Object> context = new HashMap<>();
        context.put("errorMessage", errorMessage);
        context.put("timestamp", LocalDateTime.now());
        if (jobExecution != null) {
            context.put("jobExecutionId", jobExecution.getId());
        }

        webhookNotificationService.sendCrawlingFailureNotification(context);
    }
}
