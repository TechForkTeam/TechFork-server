package com.techfork.domain.recommendation.service;

import com.techfork.domain.recommendation.config.RecommendationProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MMR (Maximal Marginal Relevance) 알고리즘 구현
 * 추천 결과의 다양성을 보장하면서도 관련성을 유지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MmrService {

    private final RecommendationProperties properties;

    @Getter
    @Builder
    public static class MmrCandidate {
        private Long postId;
        private float[] titleVector;        // 제목 임베딩 벡터
        private float[] summaryVector;      // 요약 임베딩 벡터
        private double similarityScore;     // 사용자 프로필과의 유사도
    }

    @Getter
    @Builder
    public static class MmrResult {
        private Long postId;
        private double similarityScore;
        private double mmrScore;
        private int rank;
    }

    /**
     * MMR 알고리즘을 적용하여 다양성을 보장하는 추천 결과 생성
     *
     * @param candidates 초기 후보군 (유사도 내림차순 정렬되어 있음)
     * @return MMR 적용된 최종 추천 결과
     */
    public List<MmrResult> applyMmr(List<MmrCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            log.warn("MMR 후보군이 제공되지 않음");
            return List.of();
        }

        int finalSize = Math.min(properties.getMmrFinalSize(), candidates.size());
        double lambda = properties.getLambda();

        List<MmrResult> selectedResults = new ArrayList<>();
        List<MmrCandidate> remainingCandidates = new ArrayList<>(candidates);

        log.debug("MMR 선택 시작: candidates={}, finalSize={}, lambda={}",
                candidates.size(), finalSize, lambda);

        // 첫 번째는 가장 유사도가 높은 문서 선택
        MmrCandidate first = remainingCandidates.remove(0);
        selectedResults.add(MmrResult.builder()
                .postId(first.getPostId())
                .similarityScore(first.getSimilarityScore())
                .mmrScore(first.getSimilarityScore())
                .rank(1)
                .build());

        // 나머지 문서들을 MMR 점수 기반으로 선택
        while (selectedResults.size() < finalSize && !remainingCandidates.isEmpty()) {
            MmrCandidate bestCandidate = null;
            double bestMmrScore = Double.NEGATIVE_INFINITY;
            int bestIndex = -1;

            for (int i = 0; i < remainingCandidates.size(); i++) {
                MmrCandidate candidate = remainingCandidates.get(i);
                double mmrScore = calculateMmrScore(candidate, selectedResults, lambda, candidates);

                if (mmrScore > bestMmrScore) {
                    bestMmrScore = mmrScore;
                    bestCandidate = candidate;
                    bestIndex = i;
                }
            }

            if (bestCandidate != null) {
                remainingCandidates.remove(bestIndex);
                selectedResults.add(MmrResult.builder()
                        .postId(bestCandidate.getPostId())
                        .similarityScore(bestCandidate.getSimilarityScore())
                        .mmrScore(bestMmrScore)
                        .rank(selectedResults.size() + 1)
                        .build());
            }
        }

        log.info("MMR 선택 완료: 전체 {} 후보 중 {} 개 선택",
                candidates.size(), selectedResults.size());

        return selectedResults;
    }

    /**
     * MMR 점수 계산
     * MMR = λ × Sim(D, Q) - (1-λ) × max[Sim(D, Di)]
     *
     * @param candidate 평가 대상 후보
     * @param selectedResults 이미 선택된 결과들
     * @param lambda 관련성/다양성 가중치
     * @return MMR 점수
     */
    private double calculateMmrScore(MmrCandidate candidate,
                                    List<MmrResult> selectedResults,
                                    double lambda,
                                    List<MmrCandidate> allCandidates) {
        // 관련성: 사용자 프로필과의 유사도 (이미 계산됨)
        double relevance = candidate.getSimilarityScore();

        // 다양성: 이미 선택된 문서들과의 최대 유사도
        double maxSimilarity = 0.0;

        if (!selectedResults.isEmpty()) {
            for (MmrResult selected : selectedResults) {
                // 선택된 문서의 벡터 찾기
                MmrCandidate selectedCandidate = allCandidates.stream()
                        .filter(c -> c.getPostId().equals(selected.getPostId()))
                        .findFirst()
                        .orElse(null);

                if (selectedCandidate != null) {
                    // 가중 평균 유사도 계산 (제목 + 요약)
                    double similarity = calculateWeightedSimilarity(candidate, selectedCandidate);
                    maxSimilarity = Math.max(maxSimilarity, similarity);
                }
            }
        }

        // MMR = λ × 관련성 - (1-λ) × 다양성 페널티
        return lambda * relevance - (1 - lambda) * maxSimilarity;
    }

    /**
     * 두 문서 간 가중 평균 유사도 계산
     * 제목과 요약을 모두 고려하여 더 정확한 다양성 측정
     *
     * @param candidate1 문서 1
     * @param candidate2 문서 2
     * @return 가중 평균 유사도
     */
    private double calculateWeightedSimilarity(MmrCandidate candidate1, MmrCandidate candidate2) {
        RecommendationProperties.EmbeddingWeights weights = properties.getEmbeddingWeights();

        double titleSim = 0.0;
        double summarySim = 0.0;

        if (candidate1.getTitleVector() != null && candidate2.getTitleVector() != null) {
            titleSim = cosineSimilarity(candidate1.getTitleVector(), candidate2.getTitleVector());
        }

        if (candidate1.getSummaryVector() != null && candidate2.getSummaryVector() != null) {
            summarySim = cosineSimilarity(candidate1.getSummaryVector(), candidate2.getSummaryVector());
        }

        // 가중 평균 (제목 + 요약만, 콘텐츠는 제외)
        double titleWeight = weights.getTitle();
        double summaryWeight = weights.getSummary();
        double totalWeight = titleWeight + summaryWeight;

        return (titleWeight * titleSim + summaryWeight * summarySim) / totalWeight;
    }

    /**
     * 코사인 유사도 계산
     */
    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length || vectorA.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += (double) vectorA[i] * vectorB[i];
            normA += (double) vectorA[i] * vectorA[i];
            normB += (double) vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
