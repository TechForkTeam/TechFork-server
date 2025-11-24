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
     * 랜덤 시드를 포함한 가중치 벡터 쿼리 생성
     * (추천 재생성 시 다양성 확보용)
     *
     * @param titleField 제목 벡터 필드명
     * @param summaryField 요약 벡터 필드명
     * @param contentChunksPath content chunks nested 경로
     * @param chunkEmbeddingField chunk 임베딩 필드명
     * @param queryVector 쿼리 벡터
     * @param titleWeight 제목 가중치
     * @param summaryWeight 요약 가중치
     * @param contentWeight 컨텐츠 가중치
     * @param randomSeed 랜덤 시드
     * @param randomWeight 랜덤 가중치 (0.0~1.0, 보통 0.1~0.3)
     * @return 랜덤 요소가 포함된 복합 쿼리
     */
    Query createWeightedVectorQueryWithRandomness(
            String titleField,
            String summaryField,
            String contentChunksPath,
            String chunkEmbeddingField,
            float[] queryVector,
            float titleWeight,
            float summaryWeight,
            float contentWeight,
            long randomSeed,
            double randomWeight
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
