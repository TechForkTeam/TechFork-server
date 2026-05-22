package com.techfork.domain.source.config;

import com.techfork.post.application.batch.PostEmbeddingProcessor;
import com.techfork.post.application.batch.PostSummaryProcessor;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.batch.PostEmbeddingReader;
import com.techfork.post.infrastructure.batch.PostEmbeddingWriter;
import com.techfork.post.infrastructure.batch.PostSummaryReader;
import com.techfork.post.infrastructure.batch.PostSummaryWriter;
import com.techfork.domain.source.batch.PostBatchWriter;
import com.techfork.domain.source.batch.RssFeedReader;
import com.techfork.domain.source.batch.RssToPostProcessor;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.listener.RssCrawlingJobListener;
import com.techfork.global.filter.MdcTaskDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * RSS 크롤링 Job 설정
 *
 * Step 1: RSS 피드 수집 및 저장
 * - Reader: RSS 피드를 블로그별로 수집
 * - Processor: 중복 체크 및 Post 엔티티 변환
 * - Writer: Post를 DB에 저장 (Bulk Insert)
 *
 * Step 2: 요약 추출 (비동기 처리)
 * - Reader: 요약이 없는 Post 조회 (단일 스레드)
 * - Processor: GPT API로 구조화된 요약 추출 (멀티 스레드)
 * - Writer: PostSummary 저장
 *
 * Step 3: 임베딩 생성 및 Elasticsearch 저장 (비동기 처리)
 * - Reader: 요약이 완료되고 임베딩되지 않은 Post 조회 (단일 스레드)
 * - Processor: Chunk 분할 + OpenAI 임베딩 생성 (멀티 스레드)
 * - Writer: Elasticsearch에 PostDocument 저장
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

    private final PostSummaryReader postSummaryReader;
    private final PostSummaryProcessor postSummaryProcessor;
    private final PostSummaryWriter postSummaryWriter;

    private final PostEmbeddingReader postEmbeddingReader;
    private final PostEmbeddingProcessor postEmbeddingProcessor;
    private final PostEmbeddingWriter postEmbeddingWriter;

    private final RssCrawlingJobListener rssCrawlingJobListener;

    @Bean
    public Job rssCrawlingJob() {
        return new JobBuilder("rssCrawlingJob", jobRepository)
                .listener(rssCrawlingJobListener)
                .start(fetchAndSaveRssStep())
                .next(extractSummaryStep())
                .next(embedAndIndexStep())
                .build();
    }

    @Bean
    public Job summaryAndEmbeddingJob() {
        return new JobBuilder("summaryAndEmbeddingJob", jobRepository)
                .start(extractSummaryStep())
                .next(embedAndIndexStep())
                .build();
    }

    @Bean
    public Step fetchAndSaveRssStep() {
        return new StepBuilder("fetchAndSaveRssStep", jobRepository)
                .<RssFeedItem, Post>chunk(10, transactionManager)
                .reader(rssFeedReader)
                .processor(rssToPostProcessor)
                .writer(postBatchWriter)
                .faultTolerant()
                // 건너뛰기 정책: 최대 10개 아이템까지 건너뛰기 허용
                .skipLimit(10)
                .skip(Exception.class)
                .noSkip(IllegalStateException.class)
                .build();
    }

    @Bean
    public Step extractSummaryStep() {
        return new StepBuilder("extractSummaryStep", jobRepository)
                .<Post, Future<Post>>chunk(5, transactionManager) // Rate Limiter(15/min)를 고려한 chunk size
                .reader(postSummaryReader)
                .processor(asyncSummaryProcessor())
                .writer(asyncSummaryWriter())
                // Resilience4j가 Retry를 담당
                // Skip 로직만 유지: 실패한 아이템을 건너뛰고 다음 아이템 처리
                .faultTolerant()
                .skipLimit(10)  // 실패 허용 개수 증가
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step embedAndIndexStep() {
        return new StepBuilder("embedAndIndexStep", jobRepository)
                .<Post, Future<PostDocument>>chunk(20, transactionManager) // 5개씩 배치 처리
                .reader(postEmbeddingReader)
                .processor(asyncEmbeddingProcessor())
                .writer(asyncEmbeddingWriter())
                // Resilience4j가 Retry를 담당
                // Skip 로직만 유지: 실패한 아이템을 건너뛰고 다음 아이템 처리
                .faultTolerant()
                .skipLimit(20)  // 임베딩 실패 허용 개수
                .skip(Exception.class)
                .build();
    }


    @Bean
    public ItemProcessor<Post, Future<Post>> asyncSummaryProcessor() {
        AsyncItemProcessor<Post, Post> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(postSummaryProcessor);
        asyncItemProcessor.setTaskExecutor(summaryTaskExecutor());
        return asyncItemProcessor;
    }

    @Bean
    public ItemWriter<Future<Post>> asyncSummaryWriter() {
        AsyncItemWriter<Post> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(postSummaryWriter);
        return asyncItemWriter;
    }

    @Bean
    public ItemProcessor<Post, Future<PostDocument>> asyncEmbeddingProcessor() {
        AsyncItemProcessor<Post, PostDocument> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(postEmbeddingProcessor);
        asyncItemProcessor.setTaskExecutor(embeddingTaskExecutor());
        return asyncItemProcessor;
    }

    @Bean
    public ItemWriter<Future<PostDocument>> asyncEmbeddingWriter() {
        AsyncItemWriter<PostDocument> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(postEmbeddingWriter);
        return asyncItemWriter;
    }

    @Bean(name = "rssFetchTaskExecutor")
    public AsyncTaskExecutor rssFetchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("rss-fetch-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskExecutor summaryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("summary-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskExecutor embeddingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("embedding-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

}
