package com.techfork.domain.recommendation.evaluation;

import com.techfork.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MMR Lambda 파라미터 최적화 테스트
 */
@Tag("evaluation")
@Slf4j
public class LambdaOptimizationTest extends RecommendationTestBase {

    @Test
    @DisplayName("Lambda 최적화 - Ground-Truth 기반 평가")
    void optimizeLambdaWithGroundTruth() {
        log.info("===== Lambda 최적화 테스트 (Ground-Truth 기반) =====");
        
        if (cachedGroundTruth == null || cachedGroundTruth.isEmpty()) {
            log.warn("Ground-Truth 데이터가 없습니다. Fixture 로드를 확인하세요.");
            return;
        }

        log.info("가중치 고정: 제목(0.5) + 요약(0.5)");
        log.info("Lambda 범위: 0.0 ~ 1.0 (0.1 단위)");

        List<ConfigCombo> configs = createLambdaTestConfigs();
        List<User> testUsers = getTestUsers();
        log.info("테스트 사용자: {} 명", testUsers.size());

        printLambdaOptimizationHeader();
        List<EvaluationResult> results = configs.stream()
                .map(config -> {
                    EvaluationResult result = evaluateConfigWithGroundTruthAndILD(config, testUsers);
                    printLambdaOptimizationResult(result);
                    return result;
                })
                .toList();
        
        printBestLambdaResults(results);
    }

    private List<ConfigCombo> createLambdaTestConfigs() {
        List<ConfigCombo> configs = new ArrayList<>();
        // Lambda 0.0 ~ 1.0 (0.1 단위)
        for (int i = 0; i <= 10; i++) {
            double lambda = i / 10.0;
            configs.add(ConfigCombo.builder()
                    .name(String.format("T0.5/S0.5 λ=%.1f", lambda))
                    .titleWeight(0.5f)
                    .summaryWeight(0.5f)
                    .contentWeight(0.0f)
                    .mmrLambda(lambda)
                    .build());
        }
        return configs;
    }

    private void printBestLambdaResults(List<EvaluationResult> results) {
        log.info("\n===== Lambda 최적화 결과 요약 (K=8 기준) =====");

        // 복합 점수 최고
        results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getCompositeScore))
                .ifPresent(best -> log.info(String.format("[복합 점수 최고] %s | R@8: %.4f | nDCG@8: %.4f | ILD: %.4f | Score: %.4f",
                        best.getConfigName(), best.getAvgRecall8(), best.getAvgNdcg8(), best.getAvgIld(), best.getCompositeScore())));

        // 다양성(ILD) 최고
        results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getAvgIld))
                .ifPresent(best -> log.info(String.format("[다양성(ILD) 최고] %s | ILD: %.4f",
                        best.getConfigName(), best.getAvgIld())));

        // Recall@8 최고
        results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getAvgRecall8))
                .ifPresent(best -> log.info(String.format("[Recall@8 최고] %s | R@8: %.4f",
                        best.getConfigName(), best.getAvgRecall8())));
    }
}
