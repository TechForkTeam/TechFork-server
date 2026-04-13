package com.techfork.domain.source.service;

import com.techfork.global.constant.RedisKey;
import com.techfork.global.lock.DistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CrawlingServiceTest {

    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job rssCrawlingJob;

    @Mock
    private DistributedLock distributedLock;

    private CrawlingService crawlingService;

    @BeforeEach
    void setUp() {
        crawlingService = new CrawlingService(jobLauncher, rssCrawlingJob, distributedLock);
    }

    @Nested
    @DisplayName("executeCrawling")
    class ExecuteCrawling {

        @Test
        @DisplayName("락을 획득하지 못하면 크롤링 job을 실행하지 않는다")
        void skipsWhenLockIsNotAcquired() {
            given(distributedLock.tryLock(RedisKey.CRAWLING_LOCK_KEY, LOCK_TTL)).willReturn(null);

            crawlingService.executeCrawling();

            verify(distributedLock).tryLock(RedisKey.CRAWLING_LOCK_KEY, LOCK_TTL);
            verifyNoInteractions(jobLauncher);
            verify(distributedLock, never()).unlock(any(), any());
        }

        @Test
        @DisplayName("락 획득 시 timestamp 파라미터와 함께 크롤링 job을 실행한다")
        void launchesJobWithTimestampWhenLockIsAcquired() throws Exception {
            String lockValue = "lock-value";
            long startedAt = System.currentTimeMillis();

            given(distributedLock.tryLock(RedisKey.CRAWLING_LOCK_KEY, LOCK_TTL)).willReturn(lockValue);
            given(jobLauncher.run(eq(rssCrawlingJob), any(JobParameters.class))).willReturn(mock(JobExecution.class));

            crawlingService.executeCrawling();

            ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
            verify(jobLauncher).run(eq(rssCrawlingJob), jobParametersCaptor.capture());
            verify(distributedLock).unlock(RedisKey.CRAWLING_LOCK_KEY, lockValue);

            JobParameters captured = jobParametersCaptor.getValue();
            assertThat(captured.getParameters()).containsKey("timestamp");
            assertThat(captured.getLong("timestamp")).isNotNull();
            assertThat(captured.getLong("timestamp")).isGreaterThanOrEqualTo(startedAt);
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("com.techfork.domain.source.service.CrawlingServiceTest#jobLauncherExceptions")
        @DisplayName("배치 예외가 발생해도 락은 항상 해제한다")
        void alwaysUnlocksWhenJobLauncherThrowsBatchExceptions(Exception exception) throws Exception {
            String lockValue = "lock-value";

            given(distributedLock.tryLock(RedisKey.CRAWLING_LOCK_KEY, LOCK_TTL)).willReturn(lockValue);
            given(jobLauncher.run(eq(rssCrawlingJob), any(JobParameters.class))).willThrow(exception);

            crawlingService.executeCrawling();

            verify(jobLauncher).run(eq(rssCrawlingJob), any(JobParameters.class));
            verify(distributedLock).unlock(RedisKey.CRAWLING_LOCK_KEY, lockValue);
        }

        @Test
        @DisplayName("예상치 못한 예외가 발생해도 락은 항상 해제한다")
        void alwaysUnlocksWhenUnexpectedExceptionOccurs() throws Exception {
            String lockValue = "lock-value";

            given(distributedLock.tryLock(RedisKey.CRAWLING_LOCK_KEY, LOCK_TTL)).willReturn(lockValue);
            given(jobLauncher.run(eq(rssCrawlingJob), any(JobParameters.class)))
                    .willThrow(new RuntimeException("unexpected"));

            crawlingService.executeCrawling();

            verify(jobLauncher).run(eq(rssCrawlingJob), any(JobParameters.class));
            verify(distributedLock).unlock(RedisKey.CRAWLING_LOCK_KEY, lockValue);
        }
    }

    private static Stream<Exception> jobLauncherExceptions() {
        return Stream.of(
                new JobExecutionAlreadyRunningException("already running"),
                new JobRestartException("restart failed"),
                new JobInstanceAlreadyCompleteException("already complete"),
                new JobParametersInvalidException("invalid parameters")
        );
    }
}
