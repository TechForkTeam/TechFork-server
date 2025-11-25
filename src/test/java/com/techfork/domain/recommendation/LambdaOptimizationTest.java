package com.techfork.domain.recommendation;

import com.techfork.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class LambdaOptimizationTest extends RecommendationTestBase {

    @Test
    @DisplayName("Lambda 최적화 - 3가지 가중치 조합 (Train/Test Split 방식)")
    void optimizeLambdaWithTrainTestSplit() {
        log.info("===== Lambda 최적화 테스트 (Train/Test Split) =====");
        log.info("읽은 글 100개 → Train 80개 (프로필 생성용) + Test 20개 (평가용)");
        log.info("가중치 조합: 컨텐츠중심, 요약중심, 기본값");
        log.info("Lambda 범위: 0.0 ~ 1.0 (0.1 단위)");

        List<ConfigCombo> configs = createLambdaTestConfigs();
        List<User> testUsers = getTestUsers();
        log.info("테스트 사용자: {} 명 (IDs: {})", testUsers.size(), TEST_USER_IDS);

        printImprovedConfigComparisonHeader();
        List<ImprovedEvaluationResult> results = evaluateAllConfigsWithTrainTestSplit(configs, testUsers);
        printBestImprovedResultByWeightType(results);
    }

    /**
     * Lambda 0.0 ~ 1.0 (0.1 단위) 테스트 설정 생성
     * 컨텐츠 중심
     */
    private List<ConfigCombo> createLambdaTestConfigs() {
        List<ConfigCombo> configs = new ArrayList<>();

        // Lambda 0.0 ~ 1.0 (0.1 단위)
        for (int i = 0; i <= 10; i++) {
            double lambda = i / 10.0;

            configs.add(ConfigCombo.builder()
                    .name(String.format("컨텐츠중심 λ=%.1f", lambda))
                    .titleWeight(0.2f)
                    .summaryWeight(0.2f)
                    .contentWeight(0.6f)
                    .mmrLambda(lambda)
                    .build());
        }

        log.info("총 {} 개 설정 생성", configs.size());
        return configs;
    }

    /**
     * 모든 설정 평가 (Train/Test Split)
     */
    private List<ImprovedEvaluationResult> evaluateAllConfigsWithTrainTestSplit(
            List<ConfigCombo> configs,
            List<User> testUsers) {
        return configs.stream()
                .map(config -> {
                    log.debug("설정 평가 시작 (Train/Test Split): {}", config.getName());
                    ImprovedEvaluationResult result = evaluateConfigWithTrainTestSplit(config, testUsers);
                    log.debug("설정 평가 완료 (Train/Test Split): {} - Recall={}, nDCG={}, ILD={}",
                            config.getName(), result.getAvgRecall(), result.getAvgNdcg(), result.getAvgIld());
                    log.info(result.toString());
                    return result;
                })
                .toList();
    }

    /**
     * 가중치 타입별 최고 성능 설정 출력 (Train/Test Split)
     */
    private void printBestImprovedResultByWeightType(List<ImprovedEvaluationResult> results) {
        log.info("\n===== 가중치 타입별 최고 성능 설정 (Train/Test Split) =====");

        // 컨텐츠 중심
        ImprovedEvaluationResult bestContent = results.stream()
                .filter(r -> r.getConfigName().startsWith("컨텐츠중심"))
                .max(Comparator.comparingDouble(ImprovedEvaluationResult::getCompositeScore))
                .orElse(null);
        if (bestContent != null) {
            log.info("\n[컨텐츠 중심 최고]");
            log.info(bestContent.toString());
        }
    }
}
