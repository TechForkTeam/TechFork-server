package com.techfork.global.elasticsearch.query;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import java.util.Set;

/**
 * Elasticsearch 벡터 검색 쿼리 빌더 인터페이스
 * 네이티브 k-NN 검색 및 하이브리드 검색을 위한 쿼리 생성 제공
 */
public interface VectorQueryBuilder {

    /**
     * 읽은 글 제외를 위한 필터 쿼리 생성 (Pre-filtering용)
     *
     * @param readPostIds 제외할 게시글 ID 목록
     * @return Elasticsearch Query 객체
     */
    Query createExcludeFilter(Set<Long> readPostIds);

    /**
     * 네이티브 k-NN 검색 객체 리스트 생성
     * (title, summary, content 필드에 대한 k-NN 검색)
     *
     * @param titleField 제목 벡터 필드명
     * @param summaryField 요약 벡터 필드명
     * @param contentField 컨텐츠 벡터 필드명 (Nested 경로 포함)
     * @param queryVector 쿼리 벡터
     * @param titleWeight 제목 가중치
     * @param summaryWeight 요약 가중치
     * @param contentWeight 컨텐츠 가중치
     * @param k 검색할 이웃 수
     * @param numCandidates 후보군 수
     * @param filter 사전 필터링 쿼리 (null 가능)
     * @return KnnSearch 객체 리스트
     */
    List<KnnSearch> createKnnSearches(
            String titleField,
            String summaryField,
            String contentField,
            float[] queryVector,
            float titleWeight,
            float summaryWeight,
            float contentWeight,
            int k,
            int numCandidates,
            Query filter
    );
}