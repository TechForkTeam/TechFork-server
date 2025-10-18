package com.techfork.domain.source.config;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.source.batch.PostBatchWriter;
import com.techfork.domain.source.batch.RssFeedReader;
import com.techfork.domain.source.batch.RssToPostProcessor;
import com.techfork.domain.source.dto.RssFeedItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * RSS 크롤링 Job 설정
 *
 * Step 1: RSS 피드 수집 및 저장
 * - Reader: RSS 피드를 블로그별로 수집
 * - Processor: 중복 체크 및 Post 엔티티 변환
 * - Writer: 배치로 DB 저장
 *
 * (추후 Step 2: 임베딩 생성, Step 3: 키워드 추출 등 추가 예정)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RssCrawlingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RssFeedReader rssFeedReader;
    private final RssToPostProcessor rssToPostProcessor;
    private final PostBatchWriter postBatchWriter;

    @Bean
    public Job rssCrawlingJob() {
        return new JobBuilder("rssCrawlingJob", jobRepository)
                .start(fetchAndSaveRssStep())
                .build();
    }

    @Bean
    public Step fetchAndSaveRssStep() {
        return new StepBuilder("fetchAndSaveRssStep", jobRepository)
                .<RssFeedItem, Post>chunk(10, transactionManager)
                .reader(rssFeedReader)
                .processor(rssToPostProcessor)
                .writer(postBatchWriter)
                // 병렬 처리: 5개 스레드로 동시에 RSS 수집
                .taskExecutor(rssTaskExecutor())
                // 재시도 정책: 네트워크 오류 발생 시 최대 3회 재시도
                .faultTolerant()
                .retryLimit(3)
                .retry(SocketTimeoutException.class)
                .retry(Exception.class)
                .noRetry(IllegalStateException.class) // TechBlog를 못 찾는 경우는 재시도 안 함
                // 건너뛰기 정책: 최대 10개 아이템까지 건너뛰기 허용
                .skipLimit(10)
                .skip(Exception.class)
                .noSkip(IllegalStateException.class)
                .build();
    }

    /**
     * RSS 수집용 스레드 풀 설정
     * - Core Pool Size: 5 (기본 5개 스레드)
     * - Max Pool Size: 10 (최대 10개 스레드)
     * - Queue Capacity: 20 (대기 큐 크기)
     */
    @Bean
    public TaskExecutor rssTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("rss-crawl-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 재시도 템플릿 설정 (필요시 사용)
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 재시도할 예외 타입과 최대 재시도 횟수 설정
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(SocketTimeoutException.class, true);
        retryableExceptions.put(Exception.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        // 재시도 간격 설정 (2초)
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
