package com.techfork.global.llm.impl;

import com.techfork.global.llm.EmbeddingClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI text-embedding-3-large 모델을 사용한 임베딩 클라이언트
 * Resilience4j를 통한 Circuit Breaker, Rate Limiter, Retry 패턴 적용
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final OpenAiEmbeddingModel embeddingModel;

    private static final int EMBEDDING_DIMENSIONS = 3072;

    @Override
    @Retry(name = "llmApi")
    @CircuitBreaker(name = "llmApi")
    @RateLimiter(name = "llmApi")
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("텍스트가 비어있습니다");
        }

        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

        if (response.getResults().isEmpty()) {
            throw new RuntimeException("임베딩 응답이 비어있습니다");
        }

        float[] embedding = response.getResults().get(0).getOutput();
        return convertToFloatList(embedding);
    }

    @Override
    @Retry(name = "llmApi")
    @CircuitBreaker(name = "llmApi")
    @RateLimiter(name = "llmApi")
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("텍스트 리스트가 비어있습니다");
        }

        EmbeddingResponse response = embeddingModel.embedForResponse(texts);

        List<List<Float>> embeddings = new ArrayList<>();
        for (var result : response.getResults()) {
            float[] embedding = result.getOutput();
            embeddings.add(convertToFloatList(embedding));
        }

        return embeddings;
    }

    /**
     * float[] 배열을 List<Float>로 변환
     */
    private List<Float> convertToFloatList(float[] embedding) {
        List<Float> result = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            result.add(value);
        }
        return result;
    }

}
