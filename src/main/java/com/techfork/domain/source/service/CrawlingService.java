package com.techfork.domain.source.service;

import com.techfork.global.constant.RedisKey;
import com.techfork.global.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * RSS 크롤링 실행 서비스
 * - Job 실행 담당
 * - Job 라이프사이클 이벤트(시작/종료)는 RssCrawlingJobListener에서 처리
 * - Redis 분산 락으로 중복 실행 방지 (스케줄러/API 공통)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    private final JobLauncher jobLauncher;
    private final Job rssCrawlingJob;
    private final DistributedLock distributedLock;

    private static final Duration LOCK_TTL = Duration.ofMinutes(30); // 크롤링 최대 실행 시간

    public void executeCrawling() {
        log.info("RSS crawling execution requested");

        String lockValue = distributedLock.tryLock(RedisKey.CRAWLING_LOCK_KEY, LOCK_TTL);

        if (lockValue == null) {
            log.warn("Another crawling job is already running. Skipping this execution.");
            return;
        }

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
        } finally {
            distributedLock.unlock(RedisKey.CRAWLING_LOCK_KEY, lockValue);
        }
    }
}
