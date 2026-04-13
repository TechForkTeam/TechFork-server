package com.techfork.domain.source.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;

@Configuration
public class WebhookNotificationConfig {

    @Bean
    @Qualifier("webhookRestOperations")
    public RestOperations webhookRestOperations(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }
}
