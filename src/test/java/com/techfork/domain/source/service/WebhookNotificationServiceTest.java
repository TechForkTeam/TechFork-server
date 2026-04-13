package com.techfork.domain.source.service;

import com.techfork.domain.source.config.WebhookNotificationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestOperations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookNotificationServiceTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class))
            .withUserConfiguration(WebhookNotificationConfig.class, WebhookNotificationService.class)
            .withPropertyValues("webhook.enabled=false");

    @Mock
    private RestOperations restOperations;

    private WebhookNotificationService webhookNotificationService;

    @BeforeEach
    void setUp() {
        webhookNotificationService = new WebhookNotificationService(restOperations);
    }

    @Test
    @DisplayName("Spring configuration으로 webhook RestOperations bean을 주입받아 생성된다")
    void createsServiceWithSpringManagedRestOperations() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebhookNotificationService.class);
            assertThat(context).hasBean("webhookRestOperations");
        });
    }

    @Nested
    @DisplayName("sendCrawlingFailureNotification")
    class SendCrawlingFailureNotification {

        @Test
        @DisplayName("webhook이 비활성화되어 있으면 아무 요청도 보내지 않는다")
        void doesNothingWhenWebhookDisabled() {
            ReflectionTestUtils.setField(webhookNotificationService, "webhookEnabled", false);

            webhookNotificationService.sendCrawlingFailureNotification(Map.of("errorMessage", "failure"));

            verify(restOperations, never()).postForEntity(any(String.class), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("webhook이 활성화되고 URL이 있으면 Discord payload를 전송한다")
        void sendsDiscordPayloadWhenEnabledAndUrlPresent() {
            ReflectionTestUtils.setField(webhookNotificationService, "webhookEnabled", true);
            ReflectionTestUtils.setField(webhookNotificationService, "discordWebhookUrl", "https://discord.example.com/webhook");

            LocalDateTime timestamp = LocalDateTime.of(2026, 4, 13, 5, 30, 45);
            Map<String, Object> context = new HashMap<>();
            context.put("errorMessage", "Job failed with status: FAILED");
            context.put("timestamp", timestamp);
            context.put("jobExecutionId", 10L);

            webhookNotificationService.sendCrawlingFailureNotification(context);

            ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restOperations).postForEntity(eq("https://discord.example.com/webhook"), requestCaptor.capture(), eq(String.class));

            HttpEntity<?> capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
            assertThat(capturedRequest.getBody()).isInstanceOf(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, String> payload = (Map<String, String>) capturedRequest.getBody();
            assertThat(payload.get("content"))
                    .contains("RSS Crawling Failed")
                    .contains("Error: Job failed with status: FAILED")
                    .contains("Job Execution ID: 10")
                    .contains("Time: 2026-04-13 05:30:45");
        }

        @Test
        @DisplayName("jobExecutionId가 없으면 N/A로 전송한다")
        void usesNaWhenJobExecutionIdMissing() {
            ReflectionTestUtils.setField(webhookNotificationService, "webhookEnabled", true);
            ReflectionTestUtils.setField(webhookNotificationService, "discordWebhookUrl", "https://discord.example.com/webhook");

            webhookNotificationService.sendCrawlingFailureNotification(Map.of(
                    "errorMessage", "Unknown error",
                    "timestamp", LocalDateTime.of(2026, 4, 13, 6, 0, 0)
            ));

            ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restOperations).postForEntity(eq("https://discord.example.com/webhook"), requestCaptor.capture(), eq(String.class));

            @SuppressWarnings("unchecked")
            Map<String, String> payload = (Map<String, String>) requestCaptor.getValue().getBody();
            assertThat(payload.get("content")).contains("Job Execution ID: N/A");
        }

        @Test
        @DisplayName("URL이 비어 있으면 전송을 시도하지 않는다")
        void doesNothingWhenWebhookUrlIsBlank() {
            ReflectionTestUtils.setField(webhookNotificationService, "webhookEnabled", true);
            ReflectionTestUtils.setField(webhookNotificationService, "discordWebhookUrl", "");

            webhookNotificationService.sendCrawlingFailureNotification(Map.of("errorMessage", "failure"));

            verify(restOperations, never()).postForEntity(any(String.class), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Discord 전송 중 예외가 나도 외부로 전파하지 않는다")
        void swallowsDiscordSendFailure() {
            ReflectionTestUtils.setField(webhookNotificationService, "webhookEnabled", true);
            ReflectionTestUtils.setField(webhookNotificationService, "discordWebhookUrl", "https://discord.example.com/webhook");
            when(restOperations.postForEntity(eq("https://discord.example.com/webhook"), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RuntimeException("discord failure"));

            assertThatCode(() -> webhookNotificationService.sendCrawlingFailureNotification(Map.of(
                    "errorMessage", "failure",
                    "timestamp", LocalDateTime.of(2026, 4, 13, 6, 30, 0)
            ))).doesNotThrowAnyException();
        }
    }
}
