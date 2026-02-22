package com.techfork.domain.recommendation.config;

import com.techfork.global.filter.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class RecommendationConfig {

    /**
     * 추천 생성 전용 executor.
     * 검색(SearchConfig)과 스레드풀을 분리하여 추천 배치 작업이 실시간 검색에 영향을 주지 않도록 한다.
     */
    @Bean(name = "recommendationAsyncExecutor")
    public Executor recommendationAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("recommendation-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}