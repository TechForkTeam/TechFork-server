package com.techfork.global.llm.impl;

import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.llm.exception.LlmException;
import com.techfork.global.llm.exception.LlmNetworkException;
import com.techfork.global.llm.exception.LlmRateLimitException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

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

    @Override
    @Retry(name = "llmApi")
    @CircuitBreaker(name = "llmApi")
    @RateLimiter(name = "llmEmbedding")
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("텍스트가 비어있습니다");
        }

        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

            if (response.getResults().isEmpty()) {
                throw new RuntimeException("임베딩 응답이 비어있습니다");
            }

            float[] embedding = response.getResults().get(0).getOutput();
            return convertToFloatList(embedding);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("OpenAI Embedding API Rate Limit exceeded: {}", e.getMessage());
                throw new LlmRateLimitException("OpenAI Embedding API rate limit exceeded", e);
            }
            log.error("OpenAI Embedding API client error: {}", e.getMessage());
            throw new LlmException("OpenAI Embedding API client error: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            log.warn("OpenAI Embedding API network error: {}", e.getMessage());
            throw new LlmNetworkException("OpenAI Embedding API network error", e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI Embedding API unexpected error: {}", e.getMessage(), e);
            throw new LlmException("OpenAI Embedding API unexpected error", e);
        }
    }

    @Override
    @Retry(name = "llmApi")
    @CircuitBreaker(name = "llmApi")
    @RateLimiter(name = "llmEmbedding")  // Embedding 생성용 Rate Limiter (28 req/min)
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("텍스트 리스트가 비어있습니다");
        }

        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);

            List<List<Float>> embeddings = new ArrayList<>();
            for (var result : response.getResults()) {
                float[] embedding = result.getOutput();
                embeddings.add(convertToFloatList(embedding));
            }

            return embeddings;
        } catch (HttpClientErrorException e) {
            // 429 Too Many Requests: Rate Limit 예외로 변환 (서킷 브레이커에서 무시됨)
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("OpenAI Embedding API Rate Limit exceeded (batch): {}", e.getMessage());
                throw new LlmRateLimitException("OpenAI Embedding API rate limit exceeded", e);
            }
            // 기타 4xx 에러는 일반 LLM 예외로 변환
            log.error("OpenAI Embedding API client error (batch): {}", e.getMessage());
            throw new LlmException("OpenAI Embedding API client error: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            // 네트워크 에러: 타임아웃, 연결 실패 등 (서킷 브레이커에서 무시되지만 재시도됨)
            log.warn("OpenAI Embedding API network error (batch): {}", e.getMessage());
            throw new LlmNetworkException("OpenAI Embedding API network error", e);
        } catch (IllegalArgumentException e) {
            // validation 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            // 기타 모든 예외는 일반 LLM 예외로 변환
            log.error("OpenAI Embedding API unexpected error (batch): {}", e.getMessage(), e);
            throw new LlmException("OpenAI Embedding API unexpected error", e);
        }
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
