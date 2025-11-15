package com.techfork.global.llm;

import java.util.List;

/**
 * 텍스트 임베딩 생성 클라이언트 인터페이스
 */
public interface EmbeddingClient {

    /**
     * 단일 텍스트를 임베딩 벡터로 변환
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터
     */
    List<Float> embed(String text);

    /**
     * 여러 텍스트를 배치로 임베딩 벡터로 변환
     *
     * @param texts 임베딩할 텍스트 리스트
     * @return 임베딩 벡터 리스트
     */
    List<List<Float>> embedBatch(List<String> texts);
}
