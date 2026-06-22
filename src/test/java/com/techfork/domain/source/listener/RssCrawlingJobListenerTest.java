package com.techfork.domain.source.listener;

import com.techfork.domain.source.service.WebhookNotificationService;
import com.techfork.global.config.ElasticsearchCacheManager;
import com.techfork.global.constant.MdcKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RssCrawlingJobListenerTest {

    @Mock
    private WebhookNotificationService webhookNotificationService;

    @Mock
    private ElasticsearchCacheManager elasticsearchCacheManager;

    private RssCrawlingJobListener rssCrawlingJobListener;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("beforeJob")
    class BeforeJob {

        @Test
        @DisplayName("배치 MDC 컨텍스트를 설정한다")
        void jobExecutionProvided_SetsBatchMdcContext() {
            rssCrawlingJobListener = new RssCrawlingJobListener(webhookNotificationService, elasticsearchCacheManager);
            JobExecution jobExecution = new JobExecution(1L);

            rssCrawlingJobListener.beforeJob(jobExecution);

            assertThat(MDC.get(MdcKey.REQUEST_ID)).isEqualTo("batch-1");
            assertThat(MDC.get(MdcKey.USER_ID)).isEqualTo("system");
        }
    }

    @Nested
    @DisplayName("afterJob")
    class AfterJob {

        @Test
        @DisplayName("성공 시 전체 step 집계를 기준으로 로그를 남기고 Elasticsearch cache warmup을 실행한다")
        void completedJob_WarmsElasticsearchCacheUsingAggregatedStepCounts(CapturedOutput output) {
            rssCrawlingJobListener = new RssCrawlingJobListener(webhookNotificationService, elasticsearchCacheManager);
            JobExecution jobExecution = new JobExecution(1L);
            jobExecution.setStatus(BatchStatus.COMPLETED);
            jobExecution.setStartTime(LocalDateTime.of(2026, 4, 13, 5, 0, 0));
            jobExecution.setEndTime(LocalDateTime.of(2026, 4, 13, 5, 0, 5));

            StepExecution fetchStep = jobExecution.createStepExecution("fetchAndSaveRssStep");
            fetchStep.setReadCount(4);
            fetchStep.setWriteCount(3);
            fetchStep.setProcessSkipCount(1);

            StepExecution summaryStep = jobExecution.createStepExecution("extractSummaryStep");
            summaryStep.setReadCount(7);
            summaryStep.setWriteCount(6);
            summaryStep.setWriteSkipCount(2);

            rssCrawlingJobListener.afterJob(jobExecution);

            verify(elasticsearchCacheManager).forceMergeAndWarmupPosts();
            verify(webhookNotificationService, never()).sendCrawlingFailureNotification(anyMap());
            assertThat(output.getOut()).contains("jobExecutionId=1, total=11, success=9, failed=3, duration=5000ms");
        }

        @Test
        @DisplayName("실패 시 실패 컨텍스트를 담아 webhook 알림을 전송한다")
        void failedJob_SendsFailureWebhookWithContext() {
            rssCrawlingJobListener = new RssCrawlingJobListener(webhookNotificationService, elasticsearchCacheManager);
            JobExecution jobExecution = new JobExecution(2L);
            jobExecution.setStatus(BatchStatus.FAILED);

            rssCrawlingJobListener.afterJob(jobExecution);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
            verify(webhookNotificationService).sendCrawlingFailureNotification(contextCaptor.capture());
            verify(elasticsearchCacheManager, never()).forceMergeAndWarmupPosts();

            Map<String, Object> context = contextCaptor.getValue();
            assertThat(context.get("errorMessage")).isEqualTo("Job failed with status: FAILED");
            assertThat(context.get("jobExecutionId")).isEqualTo(2L);
            assertThat(context.get("timestamp")).isInstanceOf(LocalDateTime.class);
        }

        @Test
        @DisplayName("완료 후 MDC를 정리한다")
        void jobCompletion_ClearsMdc() {
            rssCrawlingJobListener = new RssCrawlingJobListener(webhookNotificationService, elasticsearchCacheManager);
            JobExecution jobExecution = new JobExecution(3L);
            jobExecution.setStatus(BatchStatus.COMPLETED);
            jobExecution.setStartTime(LocalDateTime.of(2026, 4, 13, 5, 0, 0));
            jobExecution.setEndTime(LocalDateTime.of(2026, 4, 13, 5, 0, 1));
            jobExecution.createStepExecution("fetchAndSaveRssStep");

            rssCrawlingJobListener.beforeJob(jobExecution);
            rssCrawlingJobListener.afterJob(jobExecution);

            assertThat(MDC.getCopyOfContextMap()).isNull();
        }
    }
}
