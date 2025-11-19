package com.techfork.domain.search;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SearchQualityService {

    /**
     * Recall@K 계산
     * 수식: (Top K 결과 중 정답 문서 개수) / (전체 정답 문서 개수)
     *
     * @param actualDocIds 검색 엔진이 반환한 문서 ID 리스트 (순서대로)
     * @param idealDocIds  판단 목록에 있는 '관련된(Relevance > 0)' 문서 ID 집합
     * @param k            평가할 상위 개수 (예: 5)
     */
    public double calculateRecall(List<String> actualDocIds, Set<String> idealDocIds, int k) {
        if (idealDocIds == null || idealDocIds.isEmpty()) {
            return 0.0;
        }

        List<String> topKResults = actualDocIds.stream()
                .limit(k)
                .toList();

        long hitCount = topKResults.stream()
                .filter(idealDocIds::contains)
                .count();

        return (double) hitCount / idealDocIds.size();
    }

    /**
     * nDCG@K 계산
     * 수식: DCG@K / IDCG@K
     *
     * @param actualDocIds    검색 엔진이 반환한 문서 ID 리스트
     * @param idealResultsMap 문서 ID별 기대 점수 (Map<DocId, RelevanceScore>)
     * @param k               평가할 상위 개수 (예: 10)
     */
    public double calculateNDCG(List<String> actualDocIds, Map<String, Integer> idealResultsMap, int k) {
        double dcg = calculateDCG(actualDocIds, idealResultsMap, k);
        double idcg = calculateIDCG(idealResultsMap, k);

        if (idcg == 0.0) return 0.0;

        return dcg / idcg;
    }

    // DCG (Discounted Cumulative Gain) 계산 - 실제 검색 결과 순서대로 점수를 매기되, 순위가 낮을수록 로그 함수로 패널티를 부여
    private double calculateDCG(List<String> actualDocIds, Map<String, Integer> idealResultsMap, int k) {
        double dcg = 0.0;

        for (int i = 0; i < Math.min(actualDocIds.size(), k); i++) {
            String docId = actualDocIds.get(i);
            int relevance = idealResultsMap.getOrDefault(docId, 0);

            // 순위 패널티 적용
            if (relevance > 0) {
                dcg += relevance / (Math.log(i + 2) / Math.log(2));
            }
        }
        return dcg;
    }

    // IDCG - 정답 문서를 점수순으로 내림차순 정렬했을 때(가장 이상적인 순서)의 DCG 값
    private double calculateIDCG(Map<String, Integer> idealResultsMap, int k) {
        List<Integer> idealRelevances = idealResultsMap.values().stream()
                .sorted(Comparator.reverseOrder()) // 점수 높은 순 정렬
                .limit(k)
                .toList();

        double idcg = 0.0;
        for (int i = 0; i < idealRelevances.size(); i++) {
            int relevance = idealRelevances.get(i);
            if (relevance > 0) {
                idcg += relevance / (Math.log(i + 2) / Math.log(2));
            }
        }
        return idcg;
    }
}