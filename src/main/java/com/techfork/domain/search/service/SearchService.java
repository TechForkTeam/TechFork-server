package com.techfork.domain.search.service;

import com.techfork.domain.search.dto.SearchResult;
import java.util.List;

public interface SearchService {

    /**
     * 1단계 일반 검색 (Retrieval)
     * - 목적: 검색 품질 평가(Recall) 및 비로그인 사용자 검색
     * - 동작: RRF(BM25 + k-NN) 하이브리드 검색만 수행하여 상위 K개 결과를 반환합니다.
     * - 개인화(Re-ranking) 로직이 적용되지 않은 순수 연관도 순입니다.
     */
    List<SearchResult> searchGeneral(String query);

    /**
     * 2단계 개인화 검색 (Re-ranking)
     * - 목적: 실제 서비스 메인 검색 (로그인 사용자용)
     * - 동작:
     * 1. 1단계 검색으로 후보군(Top 100) 확보
     * 2. 사용자의 프로필 벡터와 문서 간 유사도 계산 (Cosine Similarity)
     * 3. 1단계 점수와 2단계 점수를 가중합하여 재정렬
     */
    List<SearchResult> searchPersonalized(String query, Long userId);
}