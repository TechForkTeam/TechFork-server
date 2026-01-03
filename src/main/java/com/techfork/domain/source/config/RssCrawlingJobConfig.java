package com.techfork.domain.source.config;

import com.techfork.domain.post.batch.*;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.source.batch.PostBatchWriter;
import com.techfork.domain.source.batch.RssFeedReader;
import com.techfork.domain.source.batch.RssToPostProcessor;
import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.listener.RssCrawlingJobListener;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * RSS 크롤링 Job 설정
 *
 * Step 1: RSS 피드 수집 및 저장
 * - Reader: RSS 피드를 블로그별로 수집
 * - Processor: 중복 체크 및 Post 엔티티 변환
 * - Writer: Post를 DB에 저장 (Bulk Insert)
 *
 * Step 2: 요약 추출
 * - Reader: 요약이 없는 Post 조회
 * - Processor: GPT API로 구조화된 요약 추출
 * - Writer: PostSummary 저장 (의미 기반 검색 최적화)
 *
 * Step 3: 임베딩 생성 및 Elasticsearch 저장
 * - Reader: 요약이 완료되고 임베딩되지 않은 Post 조회
 * - Processor: Chunk 분할 + OpenAI 임베딩 생성
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
                .<Post, Post>chunk(1, transactionManager) // Rate Limit 방지를 위해 1개씩 진행
                .reader(postSummaryReader)
                .processor(postSummaryProcessor)
                .writer(postSummaryWriter)
                // LLM API 호출이 있으므로 재시도 정책 설정
                .faultTolerant()
                .retryLimit(2)
                .retry(Exception.class)
                .skipLimit(10)  // 실패 허용 개수 증가
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step embedAndIndexStep() {
        return new StepBuilder("embedAndIndexStep", jobRepository)
                .<Post, PostDocument>chunk(20, transactionManager) // 5개씩 배치 처리
                .reader(postEmbeddingReader)
                .processor(postEmbeddingProcessor)
                .writer(postEmbeddingWriter)
                // 3개씩 병렬 처리
                .taskExecutor(embeddingTaskExecutor())
                // OpenAI API 호출이 있으므로 재시도 정책 설정
                .faultTolerant()
                .retryLimit(2)
                .retry(Exception.class)
                .skipLimit(20)  // 임베딩 실패 허용 개수
                .skip(Exception.class)
                .build();
    }

    @Bean
    public TaskExecutor embeddingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("embedding-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

}
