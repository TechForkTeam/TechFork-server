package com.techfork.domain.recommendation;

import com.techfork.domain.recommendation.service.LlmRecommendationService;
import com.techfork.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

/**
 * 추천 시스템 K값별 성능 비교 테스트
 */
@Slf4j
public class RecommendationKValueTest extends RecommendationTestBase {

    private static final int[] K_VALUES = {5, 10, 20, 30};

    @Test
    @DisplayName("K 값별 성능 비교 (Recall@K, nDCG@K)")
    void compareKValues() {
        log.info("===== K 값별 성능 비교 =====");

        LlmRecommendationService service = createDefaultRecommendationService();
        List<User> testUsers = getTestUsers(DEFAULT_TEST_USER_COUNT);

        printTableHeader();
        evaluateForAllKValues(service, testUsers);
    }

    /**
     * 기본 설정으로 추천 서비스 생성
     */
    private LlmRecommendationService createDefaultRecommendationService() {
        return createRecommendationService(
                createProperties(
                        DEFAULT_TITLE_WEIGHT,
                        DEFAULT_SUMMARY_WEIGHT,
                        DEFAULT_CONTENT_WEIGHT,
                        DEFAULT_MMR_LAMBDA
                )
        );
    }

    /**
     * 모든 K 값에 대해 평가
     */
    private void evaluateForAllKValues(LlmRecommendationService service, List<User> testUsers) {
        for (int k : K_VALUES) {
            evaluateForK(service, testUsers, k);
        }
    }

    /**
     * 특정 K 값으로 평가
     */
    private void evaluateForK(LlmRecommendationService service, List<User> testUsers, int k) {
        List<UserMetrics> metrics = testUsers.stream()
                .map(user -> evaluateUser(user, service, k))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        printKResult(k, metrics);
    }

    /**
     * K값 평가 결과 출력
     */
    private void printKResult(int k, List<UserMetrics> metrics) {
        double avgRecall = metrics.stream().mapToDouble(UserMetrics::getRecall).average().orElse(0.0);
        double avgNdcg = metrics.stream().mapToDouble(UserMetrics::getNdcg).average().orElse(0.0);
        double avgIld = metrics.stream().mapToDouble(UserMetrics::getIld).average().orElse(0.0);

        log.info("%-5d | %-12s | %-12s | %-12s",
                k,
                String.format("%.4f", avgRecall),
                String.format("%.4f", avgNdcg),
                String.format("%.4f", avgIld));
    }

    /**
     * 테이블 헤더 출력
     */
    private void printTableHeader() {
        log.info("\n%-5s | %-12s | %-12s | %-12s", "K", "Recall@K", "nDCG@K", "ILD");
        log.info("-".repeat(50));
    }
}
