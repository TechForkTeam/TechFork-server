package com.techfork.domain.source.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WebhookNotificationService {

    private final RestOperations restOperations;

    @Value("${webhook.discord.url:#{null}}")
    private String discordWebhookUrl;

    @Value("${webhook.enabled:false}")
    private boolean webhookEnabled;

    public WebhookNotificationService(@Qualifier("webhookRestOperations") RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public void sendCrawlingFailureNotification(Map<String, Object> context) {
        if (!webhookEnabled) {
            log.debug("Webhook notification is disabled");
            return;
        }

        String errorMessage = (String) context.getOrDefault("errorMessage", "Unknown error");
        LocalDateTime timestamp = (LocalDateTime) context.getOrDefault("timestamp", LocalDateTime.now());
        Long jobExecutionId = (Long) context.get("jobExecutionId");

        String message = buildFailureMessage(errorMessage, timestamp, jobExecutionId);

        if (discordWebhookUrl != null && !discordWebhookUrl.isEmpty()) {
            sendDiscordNotification(message);
        }
    }

    private String buildFailureMessage(String errorMessage, LocalDateTime timestamp, Long jobExecutionId) {
        return String.format(
                "🚨 RSS Crawling Failed\n" +
                        "- Error: %s\n" +
                        "- Job Execution ID: %s\n" +
                        "- Time: %s",
                errorMessage,
                jobExecutionId != null ? jobExecutionId : "N/A",
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private void sendDiscordNotification(String message) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("content", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restOperations.postForEntity(discordWebhookUrl, request, String.class);

            log.info("Discord notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send Discord notification", e);
        }
    }
}
