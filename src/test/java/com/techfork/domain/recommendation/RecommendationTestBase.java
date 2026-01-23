package com.techfork.domain.recommendation;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.post.repository.PostDocumentRepository;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.repository.RecommendationHistoryRepository;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.recommendation.service.LlmRecommendationService;
import com.techfork.domain.recommendation.service.MmrService;
import com.techfork.domain.recommendation_quality.ImprovedRecommendationTestCase;
import com.techfork.domain.recommendation_quality.RecommendationQualityService;
import com.techfork.domain.recommendation_quality.RecommendationTestCase;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.global.elasticsearch.query.VectorQueryBuilder;
import com.techfork.global.util.TimeDecayStrategy;
import com.techfork.global.util.VectorUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

/**
 * 추천 시스템 테스트를 위한 공통 베이스 클래스
 */
@Slf4j
@SpringBootTest(properties = "spring.profiles.active=local-tunnel")
public abstract class RecommendationTestBase {

    // 테스트 상수
    protected static final int DEFAULT_K_VALUE = 10;
    protected static final int DEFAULT_TEST_USER_COUNT = 5;
    protected static final List<Long> TEST_USER_IDS = Arrays.asList(71L, 72L, 73L, 74L, 75L);
    protected static final float DEFAULT_TITLE_WEIGHT = 0.4f;
    protected static final float DEFAULT_SUMMARY_WEIGHT = 0.4f;
    protected static final float DEFAULT_CONTENT_WEIGHT = 0.2f;
    protected static final double DEFAULT_MMR_LAMBDA = 0.6;
    protected static final double RECALL_WEIGHT = 0.4;
    protected static final double NDCG_WEIGHT = 0.4;
    protected static final double ILD_WEIGHT = 0.2;

    @Autowired
    protected TestDataGenerator testDataGenerator;
    @Autowired
    protected RecommendationQualityService qualityService;
    @Autowired
    protected PostDocumentRepository postDocumentRepository;
    @Autowired
    protected ElasticsearchClient elasticsearchClient;
    @Autowired
    protected UserProfileDocumentRepository userProfileDocumentRepository;
    @Autowired
    protected RecommendedPostRepository recommendedPostRepository;
    @Autowired
    protected RecommendationHistoryRepository recommendationHistoryRepository;
    @Autowired
    protected ReadPostRepository readPostRepository;
    @Autowired
    protected PostRepository postRepository;
    @Autowired
    protected TimeDecayStrategy timeDecayStrategy;
    @Autowired
    protected VectorQueryBuilder vectorQueryBuilder;
    @Autowired
    protected com.techfork.domain.user.repository.UserRepository userRepository;

    protected static List<User> cachedTestUsers;

    /**
     * 설정 조합
     */
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

    /**
     * 평가 결과
     */
    @Data
    @Builder
    @AllArgsConstructor
    protected static class EvaluationResult {
        String configName;
        double avgRecall;
        double avgNdcg;
        double avgIld;
        double compositeScore;

        public double getOverallScore() {
            return compositeScore;
        }

        @Override
        public String toString() {
            return String.format("%-20s | Recall: %.4f | nDCG: %.4f | ILD: %.4f | Score: %.4f",
                    configName, avgRecall, avgNdcg, avgIld, compositeScore);
        }
    }

    /**
     * Train/Test Split 기반 평가 결과
     */
    @Data
    @Builder
    @AllArgsConstructor
    protected static class ImprovedEvaluationResult {
        String configName;
        double avgRecall;
        double avgNdcg;
        double avgIld;
        double compositeScore;

        @Override
        public String toString() {
            return String.format("%-20s | Recall: %.4f | nDCG: %.4f | ILD: %.4f | Score: %.4f",
                    configName, avgRecall, avgNdcg, avgIld, compositeScore);
        }
    }

    /**
     * Train/Test Split 기반 사용자별 평가 메트릭
     */
    @Data
    @AllArgsConstructor
    protected static class ImprovedUserMetrics {
        double recall;
        double ndcg;
        double ild;
    }

    /**
     * 사용자별 평가 메트릭
     */
    @Data
    @AllArgsConstructor
    protected static class UserMetrics {
        double recall;
        double ndcg;
        double ild;
    }

    /**
     * 테스트 사용자 조회 (ID 71-75번 사용자)
     */
    protected List<User> getTestUsers() {
        if (cachedTestUsers == null) {
            cachedTestUsers = userRepository.findAllWithInterestCategoriesByIds(TEST_USER_IDS);
            log.info("테스트 사용자 {} 명 로드 완료: IDs={}", cachedTestUsers.size(), TEST_USER_IDS);
        }
        return cachedTestUsers;
    }

    /**
     * 프로필이 있는 테스트 사용자 조회 (레거시, 호환성 유지)
     */
    @Deprecated
    protected List<User> getTestUsers(int count) {
        List<User> users = getTestUsers();
        return users.subList(0, Math.min(count, users.size()));
    }

    /**
     * 사용자 관심사 추출
     */
    protected List<EInterestCategory> getUserInterests(User user) {
        if (user.getInterestCategories() == null || user.getInterestCategories().isEmpty()) {
            return List.of();
        }
        return user.getInterestCategories().stream()
                .map(ic -> ic.getCategory())
                .toList();
    }

    /**
     * 복합 점수 계산
     */
    protected double calculateCompositeScore(double recall, double ndcg, double ild) {
        return recall * RECALL_WEIGHT + ndcg * NDCG_WEIGHT + ild * ILD_WEIGHT;
    }

    /**
     * 커스텀 RecommendationProperties 생성
     */
    protected RecommendationProperties createProperties(float titleWeight, float summaryWeight,
                                                        float contentWeight, double lambda) {
        RecommendationProperties props = new RecommendationProperties();
        props.setKnnSearchSize(100);
        props.setNumCandidates(200);
        props.setMmrFinalSize(30);
        props.setLambda(lambda);
        props.setActiveUserHours(24);

        RecommendationProperties.EmbeddingWeights weights = new RecommendationProperties.EmbeddingWeights();
        weights.setTitle(titleWeight);
        weights.setSummary(summaryWeight);
        weights.setContent(contentWeight);
        props.setEmbeddingWeights(weights);

        return props;
    }

    /**
     * 커스텀 LlmRecommendationService 생성
     */
    protected LlmRecommendationService createRecommendationService(RecommendationProperties props) {
        MmrService mmrService = new MmrService(props);
        return new LlmRecommendationService(
                elasticsearchClient,
                userProfileDocumentRepository,
                recommendedPostRepository,
                recommendationHistoryRepository,
                readPostRepository,
                postRepository,
                mmrService,
                timeDecayStrategy,
                props,
                vectorQueryBuilder
        );
    }

    /**
     * 게시글 ID 리스트로부터 벡터 조회
     */
    protected List<float[]> getVectorsForPosts(List<Long> postIds) {
        return postIds.stream()
                .map(postDocumentRepository::findByPostId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(doc -> VectorUtil.convertToFloatArray(doc.getSummaryEmbedding()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 평균 메트릭 계산
     */
    protected EvaluationResult calculateAverageMetrics(String configName, List<UserMetrics> metrics) {
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
     * 설정 비교용 테이블 헤더 출력
     */
    protected void printConfigComparisonHeader() {
        log.info("\n%-20s | %-14s | %-14s | %-14s | %-14s",
                "설정", "Recall@10", "nDCG@10", "ILD", "Composite");
        log.info("-".repeat(90));
    }

    /**
     * Train/Test Split 기반 단일 사용자 평가
     */
    protected Optional<ImprovedUserMetrics> evaluateUserWithTrainTestSplit(
            User user,
            LlmRecommendationService service,
            int k) {
        try {
            List<EInterestCategory> interests = getUserInterests(user);
            if (interests.isEmpty()) {
                log.debug("사용자 {} 관심사 없음", user.getId());
                return Optional.empty();
            }

            ImprovedRecommendationTestCase testCase = testDataGenerator.generateImprovedTestCase(user, interests);

            if (testCase.getTestPostIds().isEmpty()) {
                log.debug("사용자 {} Test Set 없음", user.getId());
                return Optional.empty();
            }

            // Train Set만 제외하고 추천 생성 (Test Set은 추천 후보에 포함)
            Set<Long> trainPostIdsSet = new java.util.HashSet<>(testCase.getTrainPostIds());
            List<Long> recommendedIds = service.generateRecommendationsForEvaluation(user, trainPostIdsSet);

            if (recommendedIds.isEmpty()) {
                log.debug("사용자 {} 추천 결과 없음", user.getId());
                return Optional.empty();
            }

            // Test Set의 각 글에 관련도 5점 부여 (실제로 읽은 글 = 매우 관련 있음)
            Map<Long, Integer> relevanceScores = testCase.getTestPostIds().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            postId -> postId,
                            postId -> 5
                    ));

            double recall = qualityService.calculateRecall(recommendedIds, testCase.getGroundTruthPostIds(), k);
            double ndcg = qualityService.calculateNDCG(recommendedIds, relevanceScores, k);
            List<float[]> vectors = getVectorsForPosts(recommendedIds.stream().limit(k).toList());
            double ild = qualityService.calculateILD(vectors);

            // 디버깅: 추천된 글 중 Test Set과 겹치는지 확인
            List<Long> topK = recommendedIds.stream().limit(k).toList();
            long matchCount = topK.stream()
                    .filter(testCase.getGroundTruthPostIds()::contains)
                    .count();

            log.info("===== 사용자 {} 평가 상세 =====", user.getId());
            log.info("Train Set: {} 개", testCase.getTrainPostIds().size());
            log.info("Test Set: {} 개 (Ground Truth)", testCase.getTestPostIds().size());
            log.info("추천된 글: {} 개", recommendedIds.size());
            log.info("Top-{} 중 Test Set 포함: {} 개", k, matchCount);
            log.info("Recall@{}: {:.4f}", k, recall);
            log.info("nDCG@{}: {:.4f}", k, ndcg);
            log.info("ILD: {:.4f}", ild);

            if (matchCount == 0) {
                log.warn("⚠️ Top-{}에 Test Set이 하나도 없습니다!", k);
                log.warn("Test Set ID 샘플: {}", testCase.getTestPostIds().stream().limit(5).toList());
                log.warn("추천 ID 샘플: {}", topK.stream().limit(5).toList());
            }

            return Optional.of(new ImprovedUserMetrics(recall, ndcg, ild));

        } catch (Exception e) {
            log.warn("사용자 {} 평가 중 오류", user.getId(), e);
            return Optional.empty();
        }
    }

    /**
     * Train/Test Split 기반 설정 평가
     */
    protected ImprovedEvaluationResult evaluateConfigWithTrainTestSplit(
            ConfigCombo config,
            List<User> testUsers) {
        log.debug("설정 평가 시작 (Train/Test Split): {}", config.getName());

        LlmRecommendationService service = createRecommendationService(
                createProperties(
                        config.getTitleWeight(),
                        config.getSummaryWeight(),
                        config.getContentWeight(),
                        config.getMmrLambda()
                )
        );

        List<ImprovedUserMetrics> metrics = testUsers.stream()
                .map(user -> evaluateUserWithTrainTestSplit(user, service, DEFAULT_K_VALUE))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return calculateAverageImprovedMetrics(config.getName(), metrics);
    }

    /**
     * Train/Test Split 기반 평균 메트릭 계산
     */
    protected ImprovedEvaluationResult calculateAverageImprovedMetrics(
            String configName,
            List<ImprovedUserMetrics> metrics) {
        double avgRecall = metrics.stream().mapToDouble(ImprovedUserMetrics::getRecall).average().orElse(0.0);
        double avgNdcg = metrics.stream().mapToDouble(ImprovedUserMetrics::getNdcg).average().orElse(0.0);
        double avgIld = metrics.stream().mapToDouble(ImprovedUserMetrics::getIld).average().orElse(0.0);

        // 복합 점수: Recall 40%, nDCG 40%, ILD 20%
        double composite = avgRecall * RECALL_WEIGHT + avgNdcg * NDCG_WEIGHT + avgIld * ILD_WEIGHT;

        log.debug("설정 평가 완료 (Train/Test Split): {} - Recall={}, nDCG={}, ILD={}",
                configName, avgRecall, avgNdcg, avgIld);

        return ImprovedEvaluationResult.builder()
                .configName(configName)
                .avgRecall(avgRecall)
                .avgNdcg(avgNdcg)
                .avgIld(avgIld)
                .compositeScore(composite)
                .build();
    }

    /**
     * Train/Test Split 기반 설정 비교 테이블 헤더
     */
    protected void printImprovedConfigComparisonHeader() {
        log.info("\n%-20s | %-14s | %-14s | %-14s | %-14s",
                "설정", "Recall@10", "nDCG@10", "ILD", "Composite");
        log.info("-".repeat(90));
    }
}
