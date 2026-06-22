package com.techfork.domain.source.scheduler;

import com.techfork.domain.source.service.CrawlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RssCrawlingSchedulerTest {

    @Mock
    private CrawlingService crawlingService;

    private RssCrawlingScheduler rssCrawlingScheduler;

    @BeforeEach
    void setUp() {
        rssCrawlingScheduler = new RssCrawlingScheduler(crawlingService);
    }

    @Nested
    @DisplayName("scheduleCrawling")
    class ScheduleCrawling {

        @Test
        @DisplayName("호출 시 크롤링 서비스를 실행한다")
        void scheduledInvocation_ExecutesCrawlingService() {
            rssCrawlingScheduler.scheduleCrawling();

            verify(crawlingService).executeCrawling();
        }

        @Test
        @DisplayName("메서드는 매일 5시 KST 스케줄로 설정된다")
        void methodAnnotation_HasExpectedScheduledMetadata() throws NoSuchMethodException {
            Scheduled scheduled = RssCrawlingScheduler.class
                    .getMethod("scheduleCrawling")
                    .getAnnotation(Scheduled.class);

            assertThat(scheduled).isNotNull();
            assertThat(scheduled.cron()).isEqualTo("0 0 5 * * *");
            assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
        }
    }
}
