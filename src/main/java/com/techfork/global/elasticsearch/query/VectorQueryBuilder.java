package com.techfork.global.elasticsearch.query;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Elasticsearch 벡터 검색 쿼리 빌더 인터페이스
 * script_score를 사용한 코사인 유사도 검색 쿼리 생성
 */
public interface VectorQueryBuilder {

    /**
     * 여러 벡터 필드를 가중치 합산하는 bool should 쿼리 생성
     * (title, summary, content chunks 등을 결합)
     *
     * @param titleField 제목 벡터 필드명
     * @param summaryField 요약 벡터 필드명
     * @param contentChunksPath content chunks nested 경로
     * @param chunkEmbeddingField chunk 임베딩 필드명
     * @param queryVector 쿼리 벡터
     * @param titleWeight 제목 가중치
     * @param summaryWeight 요약 가중치
     * @param contentWeight 컨텐츠 가중치
     * @return 가중치 적용된 복합 쿼리
     */
    Query createWeightedVectorQuery(
            String titleField,
            String summaryField,
            String contentChunksPath,
            String chunkEmbeddingField,
            float[] queryVector,
            float titleWeight,
            float summaryWeight,
            float contentWeight
    );

    /**
     * 단일 필드에 대한 script_score 쿼리 생성
     *
     * @param fieldName 벡터 필드명
     * @param queryVector 쿼리 벡터
     * @param boost 부스트 가중치
     * @return script_score 쿼리
     */
    Query createScriptScoreQuery(String fieldName, float[] queryVector, float boost);

    /**
     * nested 필드에 대한 script_score 쿼리 생성
     *
     * @param nestedPath nested 경로
     * @param vectorFieldName 벡터 필드명 (nested 내부)
     * @param queryVector 쿼리 벡터
     * @param boost 부스트 가중치
     * @param scoreMode nested 스코어 모드 (Max, Avg 등)
     * @return nested script_score 쿼리
     */
    Query createNestedScriptScoreQuery(
            String nestedPath,
            String vectorFieldName,
            float[] queryVector,
            float boost,
            ChildScoreMode scoreMode
    );
}
