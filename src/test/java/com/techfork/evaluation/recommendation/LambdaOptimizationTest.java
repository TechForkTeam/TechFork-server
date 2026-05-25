package com.techfork.evaluation.recommendation;

import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.useraccount.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * MMR Lambda 파라미터 최적화 테스트 (Phase 4)
 *
 * Phase 1~3에서 결정된 최적값을 고정하고, lambda만 변화시켜 테스트.
 * MMR Top-K 샘플링을 비활성화(결정적)하여 재현 가능한 평가.
 */
@Tag("evaluation")
@Slf4j
public class LambdaOptimizationTest extends RecommendationTestBase {

    private static final String REPORT_FILE = "evaluation-report-recommendation-phase4.json";

    // Phase 1 최적 가중치
    private static final float BEST_TITLE_WEIGHT = 0.6f;
    private static final float BEST_SUMMARY_WEIGHT = 0.2f;
    private static final float BEST_CONTENT_WEIGHT = 0.2f;

    // Phase 2 최적 K값
    private static final int BEST_KNN_SEARCH_SIZE = 50;
    private static final int BEST_NUM_CANDIDATES = 150;

    // Phase 3 최적 후보군 크기
    private static final int BEST_MMR_CANDIDATE_SIZE = 60;
    private static final int BEST_MMR_FINAL_SIZE = 30;

    @Test
    @DisplayName("Lambda 최적화 - Phase 1~3 최적값 고정, 결정적 MMR")
    void optimizeLambda() throws Exception {
        log.info("===== Lambda 최적화 테스트 (Phase 4) =====");
        log.info("Ground-Truth: {} 명 사용자", cachedGroundTruth.size());
        log.info("고정값: title={}, summary={}, content={}, knnSearchSize={}, numCandidates={}, mmrCandidateSize={}",
                BEST_TITLE_WEIGHT, BEST_SUMMARY_WEIGHT, BEST_CONTENT_WEIGHT,
                BEST_KNN_SEARCH_SIZE, BEST_NUM_CANDIDATES, BEST_MMR_CANDIDATE_SIZE);

        List<Double> lambdaValues = List.of(0.80, 0.85, 0.90, 0.93, 0.95, 0.97, 1.0);
        List<User> testUsers = getTestUsers();
        log.info("테스트 사용자: {} 명", testUsers.size());
        log.info("Lambda 범위: {}", lambdaValues);

        printHeader();
        List<EvaluationResult> results = evaluateAll(lambdaValues, testUsers);
        printBestResult(results);

        saveRecommendationReport(REPORT_FILE, "Lambda 최적화", true, results);
    }

    private List<EvaluationResult> evaluateAll(List<Double> lambdaValues, List<User> testUsers) {
        List<EvaluationResult> results = new ArrayList<>();

        for (Double lambda : lambdaValues) {
            RecommendationProperties props = new RecommendationProperties();
            props.setKnnSearchSize(BEST_KNN_SEARCH_SIZE);
            props.setNumCandidates(BEST_NUM_CANDIDATES);
            props.setMmrCandidateSize(BEST_MMR_CANDIDATE_SIZE);
            props.setMmrFinalSize(BEST_MMR_FINAL_SIZE);
            props.setLambda(lambda);
            props.setMmrFirstTopK(1);
            props.setMmrTopK(1);

            RecommendationProperties.EmbeddingWeights weights = new RecommendationProperties.EmbeddingWeights();
            weights.setTitle(BEST_TITLE_WEIGHT);
            weights.setSummary(BEST_SUMMARY_WEIGHT);
            weights.setContent(BEST_CONTENT_WEIGHT);
            props.setEmbeddingWeights(weights);

            String configName = String.format("λ=%.2f", lambda);

            List<UserMetrics> metrics = testUsers.stream()
                    .map(user -> evaluateUserWithGroundTruthAndILD(user, props))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            EvaluationResult result = calculateAverageMetrics(configName, metrics);
            results.add(result);
            printResult(result);
        }

        return results;
    }

    private void printHeader() {
        log.info("");
        log.info(String.format("%-12s | %-8s | %-8s | %-8s | %-8s | %-8s | %-8s | %-8s | %-10s | %-8s",
                "설정", "R@4", "R@8", "R@30", "nDCG@4", "nDCG@8", "nDCG@30", "ILD", "Composite", "Latency"));
        log.info("-".repeat(115));
    }

    private void printResult(EvaluationResult result) {
        log.info(String.format("%-12s | %.4f   | %.4f   | %.4f   | %.4f   | %.4f   | %.4f   | %.4f   | %.4f     | %.0fms",
                result.getConfigName(),
                result.getAvgRecall4(), result.getAvgRecall8(), result.getAvgRecall30(),
                result.getAvgNdcg4(), result.getAvgNdcg8(), result.getAvgNdcg30(),
                result.getAvgIld(), result.getCompositeScore(), result.getAvgLatencyMs()));
    }

    private void printBestResult(List<EvaluationResult> results) {
        log.info("");
        log.info("===== 최적 Lambda =====");

        results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getCompositeScore))
                .ifPresent(best -> log.info(String.format(
                        "[Composite 최고] %s | R@8: %.4f | nDCG@8: %.4f | ILD: %.4f | Score: %.4f | Latency: %.0fms",
                        best.getConfigName(), best.getAvgRecall8(), best.getAvgNdcg8(),
                        best.getAvgIld(), best.getCompositeScore(), best.getAvgLatencyMs())));

        results.stream()
                .max(Comparator.comparingDouble(r -> (r.getAvgRecall8() + r.getAvgNdcg8()) / 2.0))
                .ifPresent(best -> log.info(String.format(
                        "[정확성 최고 (R@8+nDCG@8)] %s | R@8: %.4f | nDCG@8: %.4f | ILD: %.4f",
                        best.getConfigName(), best.getAvgRecall8(), best.getAvgNdcg8(), best.getAvgIld())));

        results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getAvgIld))
                .ifPresent(best -> log.info(String.format(
                        "[다양성 최고 (ILD)] %s | ILD: %.4f | R@8: %.4f | nDCG@8: %.4f",
                        best.getConfigName(), best.getAvgIld(), best.getAvgRecall8(), best.getAvgNdcg8())));
    }
}