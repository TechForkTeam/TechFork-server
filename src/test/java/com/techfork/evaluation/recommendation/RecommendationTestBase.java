package com.techfork.evaluation.recommendation;

import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.post.repository.PostDocumentRepository;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.evaluation.recommendation.util.EvaluationFixtureLoader;
import com.techfork.domain.user.entity.User;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.global.util.VectorUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 추천 시스템 테스트를 위한 공통 베이스 클래스
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class RecommendationTestBase extends IntegrationTestBase {

    // 테스트 상수
    protected static final int K_FIRST_ROW = 4;
    protected static final int K_FIRST_SCREEN = 8;
    protected static final int K_DEEP_EXPLORE = 30;

    protected static final float DEFAULT_TITLE_WEIGHT = 0.4f;
    protected static final float DEFAULT_SUMMARY_WEIGHT = 0.4f;
    protected static final float DEFAULT_CONTENT_WEIGHT = 0.2f;

    protected static final double RECALL_WEIGHT = 0.4;
    protected static final double NDCG_WEIGHT = 0.4;
    protected static final double ILD_WEIGHT = 0.2;

    @Autowired protected EvaluationFixtureLoader fixtureLoader;
    @Autowired protected RecommendationQualityService qualityService;
    @Autowired protected RecommendationEvaluationService evaluationService; // 새로운 서비스
    @Autowired protected PostDocumentRepository postDocumentRepository;
    @Autowired protected ReadPostRepository readPostRepository;
    @Autowired protected com.techfork.domain.user.repository.UserRepository userRepository;

    protected static List<User> cachedTestUsers;
    protected static Map<Long, Map<Long, Integer>> cachedGroundTruth;
    private static boolean fixturesLoaded = false;

    @BeforeAll
    void loadFixtures() {
        if (!fixturesLoaded) {
            log.info("===== Fixture 데이터 로드 시작 =====");
            cachedGroundTruth = fixtureLoader.loadAll();
            fixturesLoaded = true;
            log.info("===== Fixture 데이터 로드 완료: {} 명 =====", cachedGroundTruth.size());
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    protected static class ConfigCombo {
        String name;
        float titleWeight;
        float summaryWeight;
        float contentWeight;
        double mmrLambda;
    }

    @Data
    @Builder
    @AllArgsConstructor
    protected static class EvaluationResult {
        String configName;
        double avgRecall4; double avgNdcg4;
        double avgRecall8; double avgNdcg8;
        double avgRecall30; double avgNdcg30;
        double avgIld;
        double compositeScore;
    }

    @Data
    @AllArgsConstructor
    protected static class UserMetrics {
        double recall4; double ndcg4;
        double recall8; double ndcg8;
        double recall30; double ndcg30;
        double ild;
    }

    protected List<User> getTestUsers() {
        if (cachedTestUsers == null) {
            List<Long> testUserIds = new ArrayList<>(cachedGroundTruth.keySet());
            cachedTestUsers = userRepository.findAllWithInterestCategoriesByIds(testUserIds);
        }
        return cachedTestUsers;
    }

    protected RecommendationProperties createProperties(float tw, float sw, float cw, double lambda) {
        RecommendationProperties props = new RecommendationProperties();
        props.setKnnSearchSize(100);
        props.setNumCandidates(200);
        props.setMmrFinalSize(30);
        props.setLambda(lambda);
        props.setActiveUserHours(24);
        RecommendationProperties.EmbeddingWeights weights = new RecommendationProperties.EmbeddingWeights();
        weights.setTitle(tw); weights.setSummary(sw); weights.setContent(cw);
        props.setEmbeddingWeights(weights);
        return props;
    }

    protected EvaluationResult calculateAverageMetrics(String configName, List<UserMetrics> metrics) {
        double r4 = metrics.stream().mapToDouble(UserMetrics::getRecall4).average().orElse(0.0);
        double n4 = metrics.stream().mapToDouble(UserMetrics::getNdcg4).average().orElse(0.0);
        double r8 = metrics.stream().mapToDouble(UserMetrics::getRecall8).average().orElse(0.0);
        double n8 = metrics.stream().mapToDouble(UserMetrics::getNdcg8).average().orElse(0.0);
        double r30 = metrics.stream().mapToDouble(UserMetrics::getRecall30).average().orElse(0.0);
        double n30 = metrics.stream().mapToDouble(UserMetrics::getNdcg30).average().orElse(0.0);
        double ild = metrics.stream().mapToDouble(UserMetrics::getIld).average().orElse(0.0);

        double score = r8 * RECALL_WEIGHT + n8 * NDCG_WEIGHT + ild * ILD_WEIGHT;

        return EvaluationResult.builder()
                .configName(configName)
                .avgRecall4(r4).avgNdcg4(n4)
                .avgRecall8(r8).avgNdcg8(n8)
                .avgRecall30(r30).avgNdcg30(n30)
                .avgIld(ild).compositeScore(score)
                .build();
    }

    protected Optional<UserMetrics> evaluateUserWithGroundTruth(User user, RecommendationProperties props) {
        try {
            Map<Long, Integer> groundTruth = cachedGroundTruth.get(user.getId());
            if (groundTruth == null || groundTruth.isEmpty()) return Optional.empty();

            Set<Long> readIds = readPostRepository.findRecentReadPostsByUserIdWithMinDuration(user.getId(), org.springframework.data.domain.PageRequest.of(0, 10000))
                    .stream().map(rp -> rp.getPost().getId()).collect(java.util.stream.Collectors.toSet());

            // 새로운 서비스 사용
            List<Long> recIds = evaluationService.generateRecommendationsForEvaluation(user, readIds, props);
            if (recIds.isEmpty()) return Optional.empty();

            double r4 = qualityService.calculateRecall(recIds, groundTruth.keySet(), K_FIRST_ROW);
            double n4 = qualityService.calculateNDCG(recIds, groundTruth, K_FIRST_ROW);
            double r8 = qualityService.calculateRecall(recIds, groundTruth.keySet(), K_FIRST_SCREEN);
            double n8 = qualityService.calculateNDCG(recIds, groundTruth, K_FIRST_SCREEN);
            double r30 = qualityService.calculateRecall(recIds, groundTruth.keySet(), K_DEEP_EXPLORE);
            double n30 = qualityService.calculateNDCG(recIds, groundTruth, K_DEEP_EXPLORE);

            return Optional.of(new UserMetrics(r4, n4, r8, n8, r30, n30, 0.0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    protected Optional<UserMetrics> evaluateUserWithGroundTruthAndILD(User user, RecommendationProperties props) {
        try {
            Map<Long, Integer> groundTruth = cachedGroundTruth.get(user.getId());
            if (groundTruth == null || groundTruth.isEmpty()) return Optional.empty();

            Set<Long> readIds = readPostRepository.findRecentReadPostsByUserIdWithMinDuration(user.getId(), org.springframework.data.domain.PageRequest.of(0, 10000))
                    .stream().map(rp -> rp.getPost().getId()).collect(java.util.stream.Collectors.toSet());

            // 새로운 서비스 사용
            List<Long> recIds = evaluationService.generateRecommendationsForEvaluation(user, readIds, props);
            if (recIds.isEmpty()) return Optional.empty();

            double r4 = qualityService.calculateRecall(recIds, groundTruth.keySet(), K_FIRST_ROW);
            double n4 = qualityService.calculateNDCG(recIds, groundTruth, K_FIRST_ROW);
            double r8 = qualityService.calculateRecall(recIds, groundTruth.keySet(), K_FIRST_SCREEN);
            double n8 = qualityService.calculateNDCG(recIds, groundTruth, K_FIRST_SCREEN);
            double r30 = qualityService.calculateRecall(recIds, groundTruth.keySet(), K_DEEP_EXPLORE);
            double n30 = qualityService.calculateNDCG(recIds, groundTruth, K_DEEP_EXPLORE);

            List<float[]> vectors = recIds.stream().limit(K_FIRST_SCREEN)
                    .map(id -> postDocumentRepository.findByPostId(id).map(d -> VectorUtil.convertToFloatArray(d.getSummaryEmbedding())).orElse(null))
                    .filter(Objects::nonNull).toList();
            double ild = qualityService.calculateILD(vectors);

            return Optional.of(new UserMetrics(r4, n4, r8, n8, r30, n30, ild));
        } catch (Exception e) {
            log.warn("사용자 {} 평가 중 오류: {}", user.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    protected EvaluationResult evaluateConfigWithGroundTruth(ConfigCombo config, List<User> testUsers) {
        RecommendationProperties props = createProperties(config.getTitleWeight(), config.getSummaryWeight(), config.getContentWeight(), config.getMmrLambda());

        List<UserMetrics> metrics = testUsers.stream()
                .map(user -> evaluateUserWithGroundTruth(user, props))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return calculateAverageMetrics(config.getName(), metrics);
    }

    protected EvaluationResult evaluateConfigWithGroundTruthAndILD(ConfigCombo config, List<User> testUsers) {
        RecommendationProperties props = createProperties(config.getTitleWeight(), config.getSummaryWeight(), config.getContentWeight(), config.getMmrLambda());

        List<UserMetrics> metrics = testUsers.stream()
                .map(user -> evaluateUserWithGroundTruthAndILD(user, props))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return calculateAverageMetrics(config.getName(), metrics);
    }

    protected void printWeightComparisonHeader() {
        log.info(String.format("\n%-25s | %-10s | %-10s | %-10s | %-10s | %-10s | %-10s",
                "설정", "R@4", "R@8", "R@30", "nDCG@4", "nDCG@8", "nDCG@30"));
        log.info("-".repeat(100));
    }

    protected void printWeightComparisonResult(EvaluationResult result) {
        log.info(String.format("%-25s | %.4f    | %.4f    | %.4f    | %.4f    | %.4f    | %.4f",
                result.getConfigName(),
                result.getAvgRecall4(), result.getAvgRecall8(), result.getAvgRecall30(),
                result.getAvgNdcg4(), result.getAvgNdcg8(), result.getAvgNdcg30()));
    }

    protected void printLambdaOptimizationHeader() {
        log.info(String.format("\n%-25s | %-10s | %-10s | %-10s | %-10s", "설정", "R@8", "nDCG@8", "ILD", "Composite"));
        log.info("-".repeat(75));
    }

    protected void printLambdaOptimizationResult(EvaluationResult result) {
        log.info(String.format("%-25s | %.4f    | %.4f    | %.4f    | %.4f",
                result.getConfigName(), result.getAvgRecall8(), result.getAvgNdcg8(), result.getAvgIld(), result.getCompositeScore()));
    }
}
