package com.techfork.evaluation.recommendation;

import com.techfork.useraccount.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 추천 시스템 설정별 성능 비교 테스트
 */
@Tag("evaluation")
@Slf4j
public class RecommendationConfigComparisonTest extends RecommendationTestBase {

    private static final String REPORT_FILE = "evaluation-report-recommendation-phase1.json";

    @Test
    @DisplayName("여러 설정 비교 평가 (1차 후보군, MMR bypass)")
    void compareConfigurationsWithGroundTruth() throws Exception {
        log.info("===== 설정별 성능 비교 (1차 후보군, MMR bypass) =====");
        log.info("Ground-Truth: {} 명 사용자", cachedGroundTruth.size());

        List<ConfigCombo> configs = createTestConfigs();
        List<User> testUsers = getTestUsers();
        log.info("테스트 사용자: {} 명", testUsers.size());

        printWeightComparisonHeader();
        List<EvaluationResult> results = evaluateAllConfigsCandidatesOnly(configs, testUsers);
        printBestWeightResult(results);

        // JSON 리포트 저장
        saveRecommendationReport(REPORT_FILE, "설정별 성능 비교 (1차 후보군)", false, results);
    }

    /**
     * 테스트할 설정 목록 생성
     * 가중치 조합 테스트이므로 Lambda=1.0 (관련성 100%, 다양성 제외)
     */
    private List<ConfigCombo> createTestConfigs() {
        return Arrays.asList(
                ConfigCombo.builder().name("균등 가중치")
                        .titleWeight(0.33f).summaryWeight(0.33f).contentWeight(0.34f).mmrLambda(1.0).build(),

                ConfigCombo.builder().name("제목 중심")
                        .titleWeight(0.6f).summaryWeight(0.2f).contentWeight(0.2f).mmrLambda(1.0).build(),

                ConfigCombo.builder().name("요약 중심")
                        .titleWeight(0.2f).summaryWeight(0.6f).contentWeight(0.2f).mmrLambda(1.0).build(),

                ConfigCombo.builder().name("컨텐츠 중심")
                        .titleWeight(0.2f).summaryWeight(0.2f).contentWeight(0.6f).mmrLambda(1.0).build(),

                ConfigCombo.builder().name("현재 기본값")
                        .titleWeight(DEFAULT_TITLE_WEIGHT).summaryWeight(DEFAULT_SUMMARY_WEIGHT)
                        .contentWeight(DEFAULT_CONTENT_WEIGHT).mmrLambda(1.0).build(),

                ConfigCombo.builder().name("제목+요약 중심")
                        .titleWeight(0.45f).summaryWeight(0.45f).contentWeight(0.1f).mmrLambda(1.0).build(),

                ConfigCombo.builder().name("제목+요약만 (컨텐츠 0)")
                        .titleWeight(0.5f).summaryWeight(0.5f).contentWeight(0.0f).mmrLambda(1.0).build()
        );
    }

    /**
     * 모든 설정 평가 (1차 후보군만 - MMR bypass)
     */
    private List<EvaluationResult> evaluateAllConfigsCandidatesOnly(
            List<ConfigCombo> configs,
            List<User> testUsers) {
        List<EvaluationResult> results = new ArrayList<>();

        for (ConfigCombo config : configs) {
            EvaluationResult result = evaluateConfigCandidatesOnly(config, testUsers);
            results.add(result);
            printWeightComparisonResult(result);
        }

        return results;
    }

    /**
     * 최고 성능 가중치 조합 출력 (K별 Recall, nDCG 기준)
     */
    private void printBestWeightResult(List<EvaluationResult> results) {
        log.info("\n===== 최고 성능 가중치 조합 (K=8 첫 화면 기준) =====");

        // Recall@8 최고
        EvaluationResult bestRecall8 = results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getAvgRecall8))
                .orElse(null);

        if (bestRecall8 != null) {
            log.info("\n[Recall@8 최고]");
            log.info(String.format("%-25s | R@4: %.4f | R@8: %.4f | R@30: %.4f | nDCG@4: %.4f | nDCG@8: %.4f | nDCG@30: %.4f",
                    bestRecall8.getConfigName(),
                    bestRecall8.getAvgRecall4(), bestRecall8.getAvgRecall8(), bestRecall8.getAvgRecall30(),
                    bestRecall8.getAvgNdcg4(), bestRecall8.getAvgNdcg8(), bestRecall8.getAvgNdcg30()));
        }

        // nDCG@8 최고
        EvaluationResult bestNdcg8 = results.stream()
                .max(Comparator.comparingDouble(EvaluationResult::getAvgNdcg8))
                .orElse(null);

        if (bestNdcg8 != null) {
            log.info("\n[nDCG@8 최고]");
            log.info(String.format("%-25s | R@4: %.4f | R@8: %.4f | R@30: %.4f | nDCG@4: %.4f | nDCG@8: %.4f | nDCG@30: %.4f",
                    bestNdcg8.getConfigName(),
                    bestNdcg8.getAvgRecall4(), bestNdcg8.getAvgRecall8(), bestNdcg8.getAvgRecall30(),
                    bestNdcg8.getAvgNdcg4(), bestNdcg8.getAvgNdcg8(), bestNdcg8.getAvgNdcg30()));
        }

        // 균형잡힌 설정 (Recall@8 + nDCG@8 평균)
        EvaluationResult bestBalanced = results.stream()
                .max(Comparator.comparingDouble(r -> (r.getAvgRecall8() + r.getAvgNdcg8()) / 2.0))
                .orElse(null);

        if (bestBalanced != null) {
            double balancedScore = (bestBalanced.getAvgRecall8() + bestBalanced.getAvgNdcg8()) / 2.0;
            log.info("\n[균형 점수 최고 (R@8 + nDCG@8 평균: {:.4f})]", balancedScore);
            log.info(String.format("%-25s | R@4: %.4f | R@8: %.4f | R@30: %.4f | nDCG@4: %.4f | nDCG@8: %.4f | nDCG@30: %.4f",
                    bestBalanced.getConfigName(),
                    bestBalanced.getAvgRecall4(), bestBalanced.getAvgRecall8(), bestBalanced.getAvgRecall30(),
                    bestBalanced.getAvgNdcg4(), bestBalanced.getAvgNdcg8(), bestBalanced.getAvgNdcg30()));
        }
    }
}
