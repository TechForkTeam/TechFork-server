package com.techfork.evaluation.recommendation;

import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.useraccount.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * kNN 검색 크기(k) 값에 따른 성능 및 품질 비교 테스트
 */
@Tag("evaluation")
@Slf4j
public class KValueComparisonTest extends RecommendationTestBase {

    private static final String REPORT_FILE = "evaluation-report-recommendation-phase2.json";

    @Test
    @DisplayName("knnSearchSize와 numCandidates 값 비교 평가 (1차 후보군, MMR bypass)")
    void compareKValues() throws Exception {
        log.info("===== K 값에 따른 성능 및 품질 비교 (1차 후보군, MMR bypass) =====");
        log.info("Ground-Truth: {} 명 사용자", cachedGroundTruth.size());

        List<KConfig> kConfigs = createKConfigs();
        List<User> testUsers = getTestUsers();
        log.info("테스트 사용자: {} 명", testUsers.size());

        printKComparisonHeader();
        List<KResult> results = evaluateAllKConfigs(kConfigs, testUsers);
        printBestKResult(results);

        // JSON 리포트 저장
        saveKValueReport(REPORT_FILE, "K값 성능 비교 (1차 후보군)", false, toReportEntries(results));
    }

    /**
     * 테스트할 K 값 설정 생성
     */
    private List<KConfig> createKConfigs() {
        return Arrays.asList(
                KConfig.builder().name("소형 (30/90)")
                        .knnSearchSize(30).numCandidates(90).build(),

                KConfig.builder().name("중간-하 (40/120)")
                        .knnSearchSize(40).numCandidates(120).build(),

                KConfig.builder().name("현재 (50/150)")
                        .knnSearchSize(50).numCandidates(150).build(),

                KConfig.builder().name("중간 (60/180)")
                        .knnSearchSize(60).numCandidates(180).build(),

                KConfig.builder().name("중간-상 (70/210)")
                        .knnSearchSize(70).numCandidates(210).build()
        );
    }

    /**
     * 모든 K 설정에 대해 평가 수행
     */
    private List<KResult> evaluateAllKConfigs(List<KConfig> kConfigs, List<User> testUsers) {
        List<KResult> results = new ArrayList<>();

        for (KConfig kConfig : kConfigs) {
            long startTime = System.currentTimeMillis();

            // Properties 생성
            RecommendationProperties properties = new RecommendationProperties();
            properties.setKnnSearchSize(kConfig.knnSearchSize);
            properties.setNumCandidates(kConfig.numCandidates);
            properties.setMmrCandidateSize(80);
            properties.setMmrFinalSize(30);
            properties.setLambda(1.0); // 다양성 제외, 관련성만

            // 가중치는 최적값으로 고정 (제목 중심)
            RecommendationProperties.EmbeddingWeights weights = new RecommendationProperties.EmbeddingWeights();
            weights.setTitle(0.6f);
            weights.setSummary(0.2f);
            weights.setContent(0.2f);
            properties.setEmbeddingWeights(weights);

            // 평가 수행 - MMR bypass, 1차 후보군만
            List<UserMetrics> userMetrics = testUsers.stream()
                    .map(user -> evaluateUserCandidatesOnly(user, properties))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            // 평균 메트릭 계산
            KMetrics avgMetrics = calculateAverageKMetrics(userMetrics);

            long elapsedTime = System.currentTimeMillis() - startTime;

            KResult result = KResult.builder()
                    .name(kConfig.name)
                    .knnSearchSize(kConfig.knnSearchSize)
                    .numCandidates(kConfig.numCandidates)
                    .metrics(avgMetrics)
                    .executionTimeMs(elapsedTime)
                    .build();

            results.add(result);
            printKResult(result);
        }

        return results;
    }

    private KMetrics calculateAverageKMetrics(List<UserMetrics> userMetrics) {
        double r4 = userMetrics.stream().mapToDouble(UserMetrics::getRecall4).average().orElse(0.0);
        double n4 = userMetrics.stream().mapToDouble(UserMetrics::getNdcg4).average().orElse(0.0);
        double r8 = userMetrics.stream().mapToDouble(UserMetrics::getRecall8).average().orElse(0.0);
        double n8 = userMetrics.stream().mapToDouble(UserMetrics::getNdcg8).average().orElse(0.0);
        double r30 = userMetrics.stream().mapToDouble(UserMetrics::getRecall30).average().orElse(0.0);
        double n30 = userMetrics.stream().mapToDouble(UserMetrics::getNdcg30).average().orElse(0.0);
        double latency = userMetrics.stream().mapToDouble(UserMetrics::getLatencyMs).average().orElse(0.0);

        return KMetrics.builder()
                .recallAt4(r4)
                .ndcgAt4(n4)
                .recallAt8(r8)
                .ndcgAt8(n8)
                .recallAt30(r30)
                .ndcgAt30(n30)
                .avgLatencyMs(latency)
                .build();
    }

    private void printKComparisonHeader() {
        log.info("");
        log.info("설정                           | K값       | Candidates | R@4    | R@8    | R@30   | nDCG@4 | nDCG@8 | nDCG@30 | Latency | 실행시간");
        log.info("-----------------------------------------------------------------------------------------------------------");
    }

    private void printKResult(KResult result) {
        log.info(String.format("%-30s | %-9s | %-10s | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.0fms | %dms",
                result.name,
                result.knnSearchSize,
                result.numCandidates,
                result.metrics.recallAt4,
                result.metrics.recallAt8,
                result.metrics.recallAt30,
                result.metrics.ndcgAt4,
                result.metrics.ndcgAt8,
                result.metrics.ndcgAt30,
                result.metrics.avgLatencyMs,
                result.executionTimeMs
        ));
    }

    private void printBestKResult(List<KResult> results) {
        log.info("");
        log.info("===== 최고 성능 K 값 조합 =====");

        // Recall@8 최고
        KResult bestRecall = results.stream()
                .max(Comparator.comparing(r -> r.metrics.recallAt8))
                .orElse(null);

        // nDCG@8 최고
        KResult bestNdcg = results.stream()
                .max(Comparator.comparing(r -> r.metrics.ndcgAt8))
                .orElse(null);

        // 균형 점수 최고 (Recall@8 + nDCG@8 평균)
        KResult bestBalance = results.stream()
                .max(Comparator.comparing(r -> (r.metrics.recallAt8 + r.metrics.ndcgAt8) / 2.0))
                .orElse(null);

        // 실행 시간 최단
        KResult fastest = results.stream()
                .min(Comparator.comparing(r -> r.executionTimeMs))
                .orElse(null);

        log.info("");
        log.info("[Recall@8 최고]");
        if (bestRecall != null) {
            log.info(String.format("%s (K=%d, C=%d) | R@8: %.4f | nDCG@8: %.4f | 시간: %dms",
                    bestRecall.name, bestRecall.knnSearchSize, bestRecall.numCandidates,
                    bestRecall.metrics.recallAt8, bestRecall.metrics.ndcgAt8, bestRecall.executionTimeMs));
        }

        log.info("");
        log.info("[nDCG@8 최고]");
        if (bestNdcg != null) {
            log.info(String.format("%s (K=%d, C=%d) | R@8: %.4f | nDCG@8: %.4f | 시간: %dms",
                    bestNdcg.name, bestNdcg.knnSearchSize, bestNdcg.numCandidates,
                    bestNdcg.metrics.recallAt8, bestNdcg.metrics.ndcgAt8, bestNdcg.executionTimeMs));
        }

        log.info("");
        log.info("[균형 점수 최고 (R@8 + nDCG@8 평균)]");
        if (bestBalance != null) {
            double balanceScore = (bestBalance.metrics.recallAt8 + bestBalance.metrics.ndcgAt8) / 2.0;
            log.info(String.format("%s (K=%d, C=%d) | R@8: %.4f | nDCG@8: %.4f | 균형: %.4f | 시간: %dms",
                    bestBalance.name, bestBalance.knnSearchSize, bestBalance.numCandidates,
                    bestBalance.metrics.recallAt8, bestBalance.metrics.ndcgAt8, balanceScore,
                    bestBalance.executionTimeMs));
        }

        log.info("");
        log.info("[실행 시간 최단]");
        if (fastest != null) {
            log.info(String.format("%s (K=%d, C=%d) | R@8: %.4f | nDCG@8: %.4f | 시간: %dms",
                    fastest.name, fastest.knnSearchSize, fastest.numCandidates,
                    fastest.metrics.recallAt8, fastest.metrics.ndcgAt8, fastest.executionTimeMs));
        }

        log.info("");
        log.info("===== 성능/품질 트레이드오프 분석 =====");
        results.forEach(r -> {
            double efficiency = (r.metrics.recallAt8 + r.metrics.ndcgAt8) / 2.0 / (r.executionTimeMs / 1000.0);
            log.info(String.format("%s: 효율성 지수 = %.4f (품질: %.4f, 시간: %.1fs)",
                    r.name,
                    efficiency,
                    (r.metrics.recallAt8 + r.metrics.ndcgAt8) / 2.0,
                    r.executionTimeMs / 1000.0
            ));
        });
    }

    private List<Map<String, Object>> toReportEntries(List<KResult> results) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (KResult r : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("configName", r.name);
            entry.put("knnSearchSize", r.knnSearchSize);
            entry.put("numCandidates", r.numCandidates);
            entry.put("averageRecall4", Math.round(r.metrics.recallAt4 * 10000.0) / 10000.0);
            entry.put("averageRecall8", Math.round(r.metrics.recallAt8 * 10000.0) / 10000.0);
            entry.put("averageRecall30", Math.round(r.metrics.recallAt30 * 10000.0) / 10000.0);
            entry.put("averageNDCG4", Math.round(r.metrics.ndcgAt4 * 10000.0) / 10000.0);
            entry.put("averageNDCG8", Math.round(r.metrics.ndcgAt8 * 10000.0) / 10000.0);
            entry.put("averageNDCG30", Math.round(r.metrics.ndcgAt30 * 10000.0) / 10000.0);
            entry.put("avgLatencyMs", Math.round(r.metrics.avgLatencyMs * 100.0) / 100.0);
            entries.add(entry);
        }
        return entries;
    }

    @Getter
    @Builder
    private static class KConfig {
        private String name;
        private int knnSearchSize;
        private int numCandidates;
    }

    @Getter
    @Builder
    private static class KMetrics {
        private double recallAt4;
        private double ndcgAt4;
        private double recallAt8;
        private double ndcgAt8;
        private double recallAt30;
        private double ndcgAt30;
        private double avgLatencyMs;
    }

    @Getter
    @Builder
    private static class KResult {
        private String name;
        private int knnSearchSize;
        private int numCandidates;
        private KMetrics metrics;
        private long executionTimeMs;
    }
}
