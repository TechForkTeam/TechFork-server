package com.techfork.global.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion (RRF) 스코어 계산 유틸리티
 * 여러 검색 결과를 결합하여 하나의 통합 점수를 생성
 * k=60 고정 사용
 */
public class RrfScorer {

    private static final int K = 60;

    /**
     * RRF 스코어 계산 (k=60 고정)
     *
     * @param resultsLists 여러 검색 결과 리스트 (각 리스트는 순위대로 정렬되어 있어야 함)
     * @param <T> 결과 항목의 타입
     * @return 각 항목의 ID와 RRF 스코어 맵
     */
    public static <T> Map<T, Double> calculateRrfScores(List<List<T>> resultsLists) {
        Map<T, Double> rrfScores = new HashMap<>();

        for (List<T> results : resultsLists) {
            for (int rank = 0; rank < results.size(); rank++) {
                T item = results.get(rank);
                double score = 1.0 / (K + rank + 1);
                rrfScores.merge(item, score, Double::sum);
            }
        }

        return rrfScores;
    }

    /**
     * 두 개의 검색 결과를 RRF로 결합 (k=60 고정)
     *
     * @param firstResults 첫 번째 검색 결과
     * @param secondResults 두 번째 검색 결과
     * @param <T> 결과 항목의 타입
     * @return 각 항목의 ID와 RRF 스코어 맵
     */
    public static <T> Map<T, Double> calculateRrfScores(List<T> firstResults, List<T> secondResults) {
        return calculateRrfScores(List.of(firstResults, secondResults));
    }
}