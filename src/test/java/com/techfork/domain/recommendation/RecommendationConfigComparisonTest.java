package com.techfork.domain.recommendation;

import com.techfork.domain.recommendation.service.LlmRecommendationService;
import com.techfork.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * 추천 시스템 설정별 성능 비교 테스트
 */
@Slf4j
public class RecommendationConfigComparisonTest extends RecommendationTestBase {

    @Test
    @DisplayName("여러 설정 비교 평가")
    void compareConfigurations() {
        log.info("===== 설정별 성능 비교 =====");

        List<ConfigCombo> configs = createTestConfigs();
        List<User> testUsers = getTestUsers(DEFAULT_TEST_USER_COUNT);

        printTableHeader();
        List<EvaluationResult> results = evaluateAllConfigs(configs, testUsers);
        printBestResult(results);
    }

    /**
     * 테스트할 설정 목록 생성
     */
    private List<ConfigCombo> createTestConfigs() {
        return Arrays.asList(
                ConfigCombo.builder().name("균등 가중치")
                        .titleWeight(0.33f).summaryWeight(0.33f).contentWeight(0.34f).mmrLambda(0.5).build(),

                ConfigCombo.builder().name("제목 중심")
                        .titleWeight(0.6f).summaryWeight(0.2f).contentWeight(0.2f).mmrLambda(0.5).build(),

                ConfigCombo.builder().name("요약 중심")
                        .titleWeight(0.2f).summaryWeight(0.6f).contentWeight(0.2f).mmrLambda(0.5).build(),

                ConfigCombo.builder().name("컨텐츠 중심")
                        .titleWeight(0.2f).summaryWeight(0.2f).contentWeight(0.6f).mmrLambda(0.5).build(),

                ConfigCombo.builder().name("현재 기본값")
                        .titleWeight(DEFAULT_TITLE_WEIGHT).summaryWeight(DEFAULT_SUMMARY_WEIGHT)
                        .contentWeight(DEFAULT_CONTENT_WEIGHT).mmrLambda(DEFAULT_MMR_LAMBDA).build(),

                ConfigCombo.builder().name("제목+요약 중심")
                        .titleWeight(0.45f).summaryWeight(0.45f).contentWeight(0.1f).mmrLambda(0.5).build(),

                ConfigCombo.builder().name("관련성 중심 (Lambda=0)")
                        .titleWeight(0.33f).summaryWeight(0.33f).contentWeight(0.34f).mmrLambda(0.0).build(),

                ConfigCombo.builder().name("다양성 중심 (Lambda=1)")
                        .titleWeight(0.33f).summaryWeight(0.33f).contentWeight(0.34f).mmrLambda(1.0).build()
        );
    }

    /**
     * 모든 설정 평가
     */
    private List<EvaluationResult> evaluateAllConfigs(List<ConfigCombo> configs, List<User> testUsers) {
        List<EvaluationResult> results = new ArrayList<>();

        for (ConfigCombo config : configs) {
            EvaluationResult result = evaluateConfig(config, testUsers);
            results.add(result);
            log.info(result.toString());
        }

        return results;
    }

    /**
     * 특정 설정으로 평가
     */
    private EvaluationResult evaluateConfig(ConfigCombo config, List<User> testUsers) {
        log.debug("설정 평가 시작: {}", config.getName());

        LlmRecommendationService service = createRecommendationService(
                createProperties(
                        config.getTitleWeight(),
                        config.getSummaryWeight(),
                        config.getContentWeight(),
                        config.getMmrLambda()
                )
        );

        List<UserMetrics> metrics = testUsers.stream()
                .map(user -> evaluateUser(user, service, DEFAULT_K_VALUE))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return calculateAverageMetrics(config.getName(), metrics);
    }

    /**
     * 평균 메트릭 계산
     */
    private EvaluationResult calculateAverageMetrics(String configName, List<UserMetrics> metrics) {
        double avgRecall = metrics.stream().mapToDouble(UserMetrics::getRecall).average().orElse(0.0);
        double avgNdcg = metrics.stream().mapToDouble(UserMetrics::getNdcg).average().orElse(0.0);
        double avgIld = metrics.stream().mapToDouble(UserMetrics::getIld).average().orElse(0.0);
        double composite = calculateCompositeScore(avgRecall, avgNdcg, avgIld);

        log.debug("설정 평가 완료: {} - Recall={}, nDCG={}, ILD={}", configName, avgRecall, avgNdcg, avgIld);

        return EvaluationResult.builder()
                .configName(configName)
                .avgRecall(avgRecall)
                .avgNdcg(avgNdcg)
                .avgIld(avgIld)
                .compositeScore(composite)
                .build();
    }

    /**
     * 테이블 헤더 출력
     */
    private void printTableHeader() {
        log.info("\n%-20s | %-14s | %-14s | %-14s | %-14s",
                "설정", "Recall@10", "nDCG@10", "ILD", "Composite");
        log.info("-".repeat(90));
    }

    /**
     * 최고 성능 설정 출력
     */
    private void printBestResult(List<EvaluationResult> results) {
        EvaluationResult best = results.stream()
                .max(Comparator.comparing(EvaluationResult::getCompositeScore))
                .orElseThrow(() -> new IllegalStateException("평가 결과가 없습니다"));

        log.info("\n===== 최고 성능 설정 =====");
        log.info(best.toString());
    }
}
