package com.techfork.domain.recommendation_quality;

import com.techfork.global.util.VectorUtil;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 추천 시스템 품질 평가 서비스
 * - Recall@K: 정답 아이템 재현율
 * - nDCG@K: 순위 기반 정확도
 * - ILD (Intra-List Diversity): 추천 목록 내 다양성
 */
@Service
public class RecommendationQualityService {

    /**
     * Recall@K 계산
     * 수식: (Top K 결과 중 정답 문서 개수) / (전체 정답 문서 개수)
     *
     * @param recommendedIds 추천된 게시글 ID 리스트 (순서대로)
     * @param relevantIds    실제 관련있는(정답) 게시글 ID 집합
     * @param k              평가할 상위 개수 (예: 10)
     */
    public double calculateRecall(List<Long> recommendedIds, Set<Long> relevantIds, int k) {
        if (relevantIds == null || relevantIds.isEmpty()) {
            return 0.0;
        }

        List<Long> topKResults = recommendedIds.stream()
                .limit(k)
                .toList();

        long hitCount = topKResults.stream()
                .filter(relevantIds::contains)
                .count();

        return (double) hitCount / relevantIds.size();
    }

    /**
     * nDCG@K 계산
     * 수식: DCG@K / IDCG@K
     * DCG는 상위 결과일수록 가중치를 높게 부여
     *
     * @param recommendedIds  추천된 게시글 ID 리스트
     * @param relevanceScores 게시글 ID별 관련도 점수 (Map<PostId, RelevanceScore>)
     *                        점수가 높을수록 더 관련있음 (예: 1~5점)
     * @param k               평가할 상위 개수 (예: 10)
     */
    public double calculateNDCG(List<Long> recommendedIds, Map<Long, Integer> relevanceScores, int k) {
        double dcg = calculateDCG(recommendedIds, relevanceScores, k);
        double idcg = calculateIDCG(relevanceScores, k);

        if (idcg == 0.0) return 0.0;

        return dcg / idcg;
    }

    /**
     * ILD (Intra-List Diversity) 계산
     * 추천 목록 내 아이템 간 평균 비유사도 (1 - 평균 코사인 유사도)
     * 값이 클수록 다양한 추천
     *
     * @param itemVectors 추천된 아이템들의 벡터 리스트 (순서대로)
     * @return 0.0 ~ 1.0 사이 값 (1.0에 가까울수록 다양함)
     */
    public double calculateILD(List<float[]> itemVectors) {
        if (itemVectors == null || itemVectors.size() <= 1) {
            return 0.0;
        }

        List<float[]> validVectors = itemVectors.stream()
                .filter(Objects::nonNull)
                .filter(v -> v.length > 0)
                .toList();

        if (validVectors.size() <= 1) {
            return 0.0;
        }

        double totalSimilarity = 0.0;
        int pairCount = 0;

        // 모든 아이템 쌍의 유사도 계산
        for (int i = 0; i < validVectors.size(); i++) {
            for (int j = i + 1; j < validVectors.size(); j++) {
                double similarity = VectorUtil.cosineSimilarity(validVectors.get(i), validVectors.get(j));
                totalSimilarity += similarity;
                pairCount++;
            }
        }

        if (pairCount == 0) {
            return 0.0;
        }

        double avgSimilarity = totalSimilarity / pairCount;
        return 1.0 - avgSimilarity; // 비유사도 = 1 - 유사도
    }

    /**
     * DCG (Discounted Cumulative Gain) 계산
     */
    private double calculateDCG(List<Long> recommendedIds, Map<Long, Integer> relevanceScores, int k) {
        double dcg = 0.0;

        for (int i = 0; i < Math.min(recommendedIds.size(), k); i++) {
            Long postId = recommendedIds.get(i);
            int relevance = relevanceScores.getOrDefault(postId, 0);

            if (relevance > 0) {
                // DCG 공식: rel_i / log2(i + 2)
                dcg += relevance / (Math.log(i + 2) / Math.log(2));
            }
        }
        return dcg;
    }

    /**
     * IDCG (Ideal DCG) 계산
     * 이상적인 순서(관련도가 높은 순)로 정렬했을 때의 DCG
     */
    private double calculateIDCG(Map<Long, Integer> relevanceScores, int k) {
        List<Integer> idealRelevances = relevanceScores.values().stream()
                .sorted(Comparator.reverseOrder())
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
