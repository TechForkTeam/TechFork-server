package com.techfork.domain.post.scheduler;

import com.techfork.global.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 메타데이터 추출에 실패한 게시글을 재처리하는 스케줄러
 * - 매일 새벽 3시에 실행
 * - Redis 분산 락으로 중복 실행 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataRetryScheduler {

    private final JobLauncher jobLauncher;
    private final Job metadataRetryJob;
    private final DistributedLock distributedLock;

    private static final String RETRY_LOCK_KEY = "metadata-retry";
    private static final Duration LOCK_TTL = Duration.ofHours(1); // 재처리 최대 실행 시간

    /**
     * 매일 새벽 3시에 메타데이터 재처리 실행
     * cron: 0 0 3 * * * -> 매일 새벽 3시
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void retryMetadataExtraction() {
        log.info("메타데이터 재처리 스케줄러 시작");

        String lockValue = distributedLock.tryLock(RETRY_LOCK_KEY, LOCK_TTL);

        if (lockValue == null) {
            log.warn("다른 메타데이터 재처리 작업이 실행 중입니다. 건너뜁니다.");
            return;
        }

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(metadataRetryJob, jobParameters);
            log.info("메타데이터 재처리 작업 완료");

        } catch (Exception e) {
            log.error("메타데이터 재처리 중 오류 발생", e);
        } finally {
            distributedLock.unlock(RETRY_LOCK_KEY, lockValue);
        }
    }
}
