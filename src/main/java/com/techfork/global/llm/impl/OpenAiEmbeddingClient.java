package com.techfork.global.llm.impl;

import com.techfork.global.llm.EmbeddingClient;
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
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final OpenAiEmbeddingModel embeddingModel;

    private static final int EMBEDDING_DIMENSIONS = 3072;

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            log.warn("빈 텍스트에 대한 임베딩 요청");
            return createZeroEmbedding();
        }

        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

            if (response.getResults().isEmpty()) {
                log.error("임베딩 응답이 비어있음");
                return createZeroEmbedding();
            }

            float[] embedding = response.getResults().get(0).getOutput();
            return convertToFloatList(embedding);

        } catch (Exception e) {
            log.error("OpenAI 임베딩 생성 실패: {}", e.getMessage(), e);
            return createZeroEmbedding();
        }
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);

            List<List<Float>> embeddings = new ArrayList<>();
            for (var result : response.getResults()) {
                float[] embedding = result.getOutput();
                embeddings.add(convertToFloatList(embedding));
            }

            return embeddings;

        } catch (Exception e) {
            log.error("OpenAI 배치 임베딩 생성 실패: {}", e.getMessage(), e);
            // 실패 시 각 텍스트마다 제로 벡터 반환
            return texts.stream()
                    .map(text -> createZeroEmbedding())
                    .toList();
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

    /**
     * 제로 벡터 생성 (오류 발생 시 대체용)
     */
    private List<Float> createZeroEmbedding() {
        List<Float> zeros = new ArrayList<>(EMBEDDING_DIMENSIONS);
        for (int i = 0; i < EMBEDDING_DIMENSIONS; i++) {
            zeros.add(0.0f);
        }
        return zeros;
    }
}
