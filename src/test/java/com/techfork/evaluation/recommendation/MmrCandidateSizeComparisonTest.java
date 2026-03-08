package com.techfork.evaluation.recommendation;

import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * MMR 후보군 크기(mmrCandidateSize)에 따른 성능 비교 테스트 (Phase 3)
 *
 * MMR은 O(n²)이므로 후보군이 클수록 다양성은 높아지지만 성능이 저하됨.
 * 정확성(Recall, nDCG)과 다양성(ILD)의 균형을 찾기 위한 테스트.
 */
@Tag("evaluation")
@Slf4j
public class MmrCandidateSizeComparisonTest extends RecommendationTestBase {

    private static final String REPORT_FILE = "evaluation-report-recommendation-phase3.json";

    // Phase 1에서 결정된 최적 가중치 고정
    private static final float BEST_TITLE_WEIGHT = 0.6f;
    private static final float BEST_SUMMARY_WEIGHT = 0.2f;
    private static final float BEST_CONTENT_WEIGHT = 0.2f;

    // Phase 2에서 결정된 최적 K값 고정
    private static final int BEST_KNN_SEARCH_SIZE = 50;
    private static final int BEST_NUM_CANDIDATES = 150;

    private static final double FIXED_LAMBDA = 0.95;

    @Test
    @DisplayName("MMR 후보군 크기별 성능 비교 (MMR 포함, ILD 측정)")
    void compareMmrCandidateSizes() throws Exception {
        log.info("===== MMR 후보군 크기별 성능 비교 =====");
        log.info("Ground-Truth: {} 명 사용자", cachedGroundTruth.size());
        log.info("고정값: title={}, summary={}, content={}, knnSearchSize={}, numCandidates={}, lambda={}",
                BEST_TITLE_WEIGHT, BEST_SUMMARY_WEIGHT, BEST_CONTENT_WEIGHT,
                BEST_KNN_SEARCH_SIZE, BEST_NUM_CANDIDATES, FIXED_LAMBDA);

        List<MmrCandidateConfig> configs = createConfigs();
        List<User> testUsers = getTestUsers();
        log.info("테스트 사용자: {} 명", testUsers.size());

        printHeader();
        List<EvaluationResult> results = evaluateAll(configs, testUsers);
        printBestResult(results);

        saveRecommendationReport(REPORT_FILE, "MMR 후보군 크기 비교", true, results);
    }

    private List<MmrCandidateConfig> createConfigs() {
        return Arrays.asList(
                MmrCandidateConfig.builder().name("후보 40개").mmrCandidateSize(40).mmrFinalSize(30).build(),
                MmrCandidateConfig.builder().name("후보 60개").mmrCandidateSize(60).mmrFinalSize(30).build(),
                MmrCandidateConfig.builder().name("현재 (80개)").mmrCandidateSize(80).mmrFinalSize(30).build(),
                MmrCandidateConfig.builder().name("후보 100개").mmrCandidateSize(100).mmrFinalSize(30).build()
        );
    }

    private List<EvaluationResult> evaluateAll(List<MmrCandidateConfig> configs, List<User> testUsers) {
        List<EvaluationResult> results = new ArrayList<>();

        for (MmrCandidateConfig config : configs) {
            RecommendationProperties props = new RecommendationProperties();
            props.setKnnSearchSize(BEST_KNN_SEARCH_SIZE);
            props.setNumCandidates(BEST_NUM_CANDIDATES);
            props.setMmrCandidateSize(config.mmrCandidateSize);
            props.setMmrFinalSize(config.mmrFinalSize);
            props.setLambda(FIXED_LAMBDA);
            props.setMmrFirstTopK(1);
            props.setMmrTopK(1);

            RecommendationProperties.EmbeddingWeights weights = new RecommendationProperties.EmbeddingWeights();
            weights.setTitle(BEST_TITLE_WEIGHT);
            weights.setSummary(BEST_SUMMARY_WEIGHT);
            weights.setContent(BEST_CONTENT_WEIGHT);
            props.setEmbeddingWeights(weights);

            List<UserMetrics> metrics = testUsers.stream()
                    .map(user -> evaluateUserWithGroundTruthAndILD(user, props))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            EvaluationResult result = calculateAverageMetrics(config.name, metrics);
            results.add(result);
            printResult(result);
        }

        return results;
    }

    private void printHeader() {
        log.info("");
        log.info(String.format("%-20s | %-8s | %-8s | %-8s | %-8s | %-8s | %-8s | %-8s | %-10s | %-8s",
                "설정", "R@4", "R@8", "R@30", "nDCG@4", "nDCG@8", "nDCG@30", "ILD", "Composite", "Latency"));
        log.info("-".repeat(120));
    }

    private void printResult(EvaluationResult result) {
        log.info(String.format("%-20s | %.4f   | %.4f   | %.4f   | %.4f   | %.4f   | %.4f   | %.4f   | %.4f     | %.0fms",
                result.getConfigName(),
                result.getAvgRecall4(), result.getAvgRecall8(), result.getAvgRecall30(),
                result.getAvgNdcg4(), result.getAvgNdcg8(), result.getAvgNdcg30(),
                result.getAvgIld(), result.getCompositeScore(), result.getAvgLatencyMs()));
    }

    private void printBestResult(List<EvaluationResult> results) {
        log.info("");
        log.info("===== 최적 MMR 후보군 크기 =====");

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

    @Getter
    @Builder
    private static class MmrCandidateConfig {
        private String name;
        private int mmrCandidateSize;
        private int mmrFinalSize;
    }
}
