package com.techfork.evaluation.recommendation;

import com.techfork.useraccount.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 제목(Title)과 요약(Summary)의 가중치 비율을 최적화하는 테스트
 * 본문(Content) 가중치는 0으로 고정하고 테스트합니다.
 */
@Tag("evaluation")
@Slf4j
public class TitleSummaryRatioOptimizationTest extends RecommendationTestBase {

    @Test
    @DisplayName("제목 vs 요약 가중치 최적화 (본문 제외)")
    void optimizeTitleSummaryRatio() {
        log.info("===== 제목 vs 요약 가중치 최적화 테스트 =====");
        log.info("조건: 본문(Content) 가중치 = 0.0, Lambda = 1.0 (순수 관련성)");
        log.info("범위: 제목 0.0~1.0, 요약 1.0~0.0 (0.1 단위)");

        List<ConfigCombo> configs = createRatioTestConfigs();
        List<User> testUsers = getTestUsers();
        log.info("테스트 사용자: {} 명", testUsers.size());

        printWeightComparisonHeader();
        List<EvaluationResult> results = new ArrayList<>();

        for (ConfigCombo config : configs) {
            EvaluationResult result = evaluateConfigWithGroundTruth(config, testUsers);
            results.add(result);
            printWeightComparisonResult(result);
        }

        printBestRatioResult(results);
    }

    /**
     * 제목 0.0~1.0 (0.1 단위) 비율 설정 생성
     */
    private List<ConfigCombo> createRatioTestConfigs() {
        List<ConfigCombo> configs = new ArrayList<>();

        for (int i = 0; i <= 10; i++) {
            float titleWeight = i / 10.0f;
            float summaryWeight = 1.0f - titleWeight;

            configs.add(ConfigCombo.builder()
                    .name(String.format("T:%.1f / S:%.1f", titleWeight, summaryWeight))
                    .titleWeight(titleWeight)
                    .summaryWeight(summaryWeight)
                    .contentWeight(0.0f)
                    .mmrLambda(1.0)
                    .build());
        }

        return configs;
    }

    /**
     * 최적 비율 결과 출력
     */
    private void printBestRatioResult(List<EvaluationResult> results) {
        log.info(" ===== 제목 vs 요약 최적 비율 결과 (K=8 기준) =====");

        // nDCG@8 최고 기준
        EvaluationResult bestNdcg8 = results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getAvgNdcg8))
                .orElse(null);

        if (bestNdcg8 != null) {
            log.info(" [nDCG@8 최고]");
            log.info(String.format("최적 설정: %s", bestNdcg8.getConfigName()));
            log.info(String.format("성능: R@8: %.4f, nDCG@8: %.4f",
                    bestNdcg8.getAvgRecall8(), bestNdcg8.getAvgNdcg8()));
        }

        // Recall@8 최고 기준
        EvaluationResult bestRecall8 = results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getAvgRecall8))
                .orElse(null);

        if (bestRecall8 != null && (bestNdcg8 == null || !bestRecall8.getConfigName().equals(bestNdcg8.getConfigName()))) {
            log.info(" [Recall@8 최고]");
            log.info(String.format("최적 설정: %s", bestRecall8.getConfigName()));
            log.info(String.format("성능: R@8: %.4f, nDCG@8: %.4f",
                    bestRecall8.getAvgRecall8(), bestRecall8.getAvgNdcg8()));
        }
    }
}
