package com.techfork.global.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    @Value("${spring.ai.anthropic.api-key}")
    private String apiKey;

    @Value("${spring.ai.anthropic.chat.options.model:claude-3-5-haiku-20241022}")
    private String model;

    @Value("${spring.ai.anthropic.chat.options.temperature:0.3}")
    private Double temperature;

    @Value("${spring.ai.anthropic.chat.options.max-tokens:8192}")
    private Integer maxTokens;

    @Bean
    public AnthropicChatModel anthropicChatModel() {
        AnthropicApi anthropicApi = AnthropicApi.builder()
                .apiKey(apiKey)
                .build();

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(options)
                .build();
    }
}
