package com.techfork.global.llm.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OpenAiConfig {
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens}")
    private Integer maxTokens;

    @Value("${spring.ai.openai.timeout:60}")
    private Integer timeoutSeconds;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        RestClient.Builder restClientBuilder = createRestClientBuilder();

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel() {
        RestClient.Builder restClientBuilder = createRestClientBuilder();

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model("text-embedding-3-large")
                .dimensions(3072)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    private RestClient.Builder createRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return RestClient.builder()
                .requestFactory(requestFactory);
    }
}
