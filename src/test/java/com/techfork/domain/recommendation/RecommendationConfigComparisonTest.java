package com.techfork.domain.recommendation;

import com.techfork.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 추천 시스템 설정별 성능 비교 테스트
 */
@Slf4j
public class RecommendationConfigComparisonTest extends RecommendationTestBase {

    @Test
    @DisplayName("여러 설정 비교 평가 (Train/Test Split 방식)")
    void compareConfigurationsWithTrainTestSplit() {
        log.info("===== 설정별 성능 비교 (Train/Test Split) =====");
        log.info("읽은 글 100개 → Train 80개 (프로필 생성용) + Test 20개 (평가용)");

        List<ConfigCombo> configs = createTestConfigs();
        List<User> testUsers = getTestUsers();
        log.info("테스트 사용자: {} 명 (IDs: {})", testUsers.size(), TEST_USER_IDS);

        printImprovedConfigComparisonHeader();
        List<ImprovedEvaluationResult> results = evaluateAllConfigsWithTrainTestSplit(configs, testUsers);
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
     * 모든 설정 평가 (Train/Test Split)
     */
    private List<ImprovedEvaluationResult> evaluateAllConfigsWithTrainTestSplit(
            List<ConfigCombo> configs,
            List<User> testUsers) {
        List<ImprovedEvaluationResult> results = new ArrayList<>();

        for (ConfigCombo config : configs) {
            ImprovedEvaluationResult result = evaluateConfigWithTrainTestSplit(config, testUsers);
            results.add(result);
            log.info(result.toString());
        }

        return results;
    }
}
