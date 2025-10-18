package com.techfork.domain.source.config;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * RSS 크롤링 Job 설정
 *
 * Step 1: RSS 피드 수집 및 저장
 * (추후 Step 2: 임베딩 생성, Step 3: 키워드 추출 등 추가 예정)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RssCrawlingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PostRepository postRepository;

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
                .writer(postWriter())
                .build();
    }

    @Bean
    public ItemWriter<Post> postWriter() {
        return items -> {
            int savedCount = 0;
            int skippedCount = 0;

            for (Post post : items) {
                if (postRepository.existsByUrl(post.getUrl())) {
                    log.debug("중복 URL 스킵: {}", post.getUrl());
                    skippedCount++;
                    continue;
                }

                postRepository.save(post);
                savedCount++;
            }

            log.info("저장: {}개, 스킵: {}개", savedCount, skippedCount);
        };
    }
}
