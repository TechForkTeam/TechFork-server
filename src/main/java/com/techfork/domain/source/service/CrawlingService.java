package com.techfork.domain.source.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

/**
 * RSS 크롤링 실행 서비스
 * - Job 실행만 담당
 * - Job 라이프사이클 이벤트(시작/종료)는 RssCrawlingJobListener에서 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    private final JobLauncher jobLauncher;
    private final Job rssCrawlingJob;

    public void executeCrawling() {
        try {
            // Job 파라미터 생성 (매번 다른 파라미터로 실행)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(rssCrawlingJob, jobParameters);

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("Job is already running", e);
        } catch (JobRestartException e) {
            log.error("Job restart failed", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("Job instance already complete", e);
        } catch (JobParametersInvalidException e) {
            log.error("Invalid job parameters", e);
        } catch (Exception e) {
            log.error("Unexpected error during crawling", e);
        }
    }
}
