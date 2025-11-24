package com.techfork.domain.recommendation;

import com.techfork.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LambdaOptimizationTest extends RecommendationTestBase {

    @Test
    @DisplayName("Lambda 최적화 - 요약 중심 vs 현재 기본값")
    void optimizeLambda() {
        log.info("===== Lambda 최적화 테스트 =====");

        List<ConfigCombo> configs = createLambdaTestConfigs();
        List<User> testUsers = getTestUsers(DEFAULT_TEST_USER_COUNT);

        printConfigComparisonHeader();
        List<EvaluationResult> results = evaluateAllConfigs(configs, testUsers);
        printBestResult(results);
    }

    /**
     * Lambda 0.0 ~ 1.0 (0.1 단위) 테스트 설정 생성
     * 요약 중심 + 현재 기본값 조합
     */
    private List<ConfigCombo> createLambdaTestConfigs() {
        List<ConfigCombo> configs = new ArrayList<>();

        // Lambda 0.0 ~ 1.0 (0.1 단위)
        for (int i = 0; i <= 10; i++) {
            double lambda = i / 10.0;

            // 1. 요약 중심 (title:0.2, summary:0.6, content:0.2)
            configs.add(ConfigCombo.builder()
                    .name(String.format("요약중심 λ=%.1f", lambda))
                    .titleWeight(0.2f)
                    .summaryWeight(0.6f)
                    .contentWeight(0.2f)
                    .mmrLambda(lambda)
                    .build());

            // 2. 현재 기본값 (title:0.4, summary:0.4, content:0.2)
            configs.add(ConfigCombo.builder()
                    .name(String.format("기본값 λ=%.1f", lambda))
                    .titleWeight(DEFAULT_TITLE_WEIGHT)
                    .summaryWeight(DEFAULT_SUMMARY_WEIGHT)
                    .contentWeight(DEFAULT_CONTENT_WEIGHT)
                    .mmrLambda(lambda)
                    .build());
        }

        return configs;
    }

    /**
     * 모든 설정 평가
     */
    private List<EvaluationResult> evaluateAllConfigs(List<ConfigCombo> configs, List<User> testUsers) {
        return configs.stream()
                .map(config -> {
                    log.debug("설정 평가 시작: {}", config.getName());
                    EvaluationResult result = evaluateConfig(config, testUsers);
                    log.debug("설정 평가 완료: {} - Recall={}, nDCG={}, ILD={}",
                            config.getName(), result.getAvgRecall(), result.getAvgNdcg(), result.getAvgIld());
                    printResult(config.getName(), result);
                    return result;
                })
                .toList();
    }

    /**
     * 최고 성능 설정 출력
     */
    private void printBestResult(List<EvaluationResult> results) {
        EvaluationResult best = results.stream()
                .max((a, b) -> Double.compare(a.getOverallScore(), b.getOverallScore()))
                .orElseThrow();

        log.info("\n===== 최고 성능 설정 =====");
        printResult(best.getConfigName(), best);
    }
}
