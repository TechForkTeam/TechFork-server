package com.techfork.domain.recommendation;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.post.repository.PostDocumentRepository;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.recommendation.repository.RecommendationHistoryRepository;
import com.techfork.domain.recommendation.service.LlmRecommendationService;
import com.techfork.domain.recommendation.service.MmrService;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 추천 시스템 테스트를 위한 공통 베이스 클래스
 */
@Slf4j
@SpringBootTest(properties = "spring.profiles.active=local-tunnel")
public abstract class RecommendationTestBase {

    // 테스트 상수
    protected static final int DEFAULT_K_VALUE = 10;
    protected static final int DEFAULT_TEST_USER_COUNT = 5;
    protected static final float DEFAULT_TITLE_WEIGHT = 0.4f;
    protected static final float DEFAULT_SUMMARY_WEIGHT = 0.4f;
    protected static final float DEFAULT_CONTENT_WEIGHT = 0.2f;
    protected static final double DEFAULT_MMR_LAMBDA = 0.6;
    protected static final double RECALL_WEIGHT = 0.4;
    protected static final double NDCG_WEIGHT = 0.4;
    protected static final double ILD_WEIGHT = 0.2;

    @Autowired protected TestDataGenerator testDataGenerator;
    @Autowired protected RecommendationQualityService qualityService;
    @Autowired protected PostDocumentRepository postDocumentRepository;
    @Autowired protected ElasticsearchClient elasticsearchClient;
    @Autowired protected UserProfileDocumentRepository userProfileDocumentRepository;
    @Autowired protected RecommendedPostRepository recommendedPostRepository;
    @Autowired protected RecommendationHistoryRepository recommendationHistoryRepository;
    @Autowired protected ReadPostRepository readPostRepository;
    @Autowired protected PostRepository postRepository;
    @Autowired protected TimeDecayStrategy timeDecayStrategy;
    @Autowired protected VectorQueryBuilder vectorQueryBuilder;

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
     * 프로필이 있는 테스트 사용자 조회
     */
    protected List<User> getTestUsers(int count) {
        if (cachedTestUsers == null || cachedTestUsers.size() < count) {
            cachedTestUsers = testDataGenerator.getUsersWithProfile(count);
        }
        return cachedTestUsers.subList(0, Math.min(count, cachedTestUsers.size()));
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
     * 단일 사용자에 대한 추천 평가
     */
    protected Optional<UserMetrics> evaluateUser(User user, LlmRecommendationService service, int k) {
        try {
            List<EInterestCategory> interests = getUserInterests(user);
            if (interests.isEmpty()) {
                log.debug("사용자 {} 관심사 없음", user.getId());
                return Optional.empty();
            }

            RecommendationTestCase testCase = testDataGenerator.generateTestCase(user, interests);
            List<Long> recommendedIds = service.generateRecommendationsForEvaluation(user);

            if (recommendedIds.isEmpty()) {
                log.debug("사용자 {} 추천 결과 없음", user.getId());
                return Optional.empty();
            }

            double recall = qualityService.calculateRecall(recommendedIds, testCase.getRelevantPostIds(), k);
            double ndcg = qualityService.calculateNDCG(recommendedIds, testCase.getGroundTruthScores(), k);
            List<float[]> vectors = getVectorsForPosts(recommendedIds.stream().limit(k).toList());
            double ild = qualityService.calculateILD(vectors);

            return Optional.of(new UserMetrics(recall, ndcg, ild));

        } catch (Exception e) {
            log.warn("사용자 {} 평가 중 오류", user.getId(), e);
            return Optional.empty();
        }
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
     * 특정 설정으로 평가
     */
    protected EvaluationResult evaluateConfig(ConfigCombo config, List<User> testUsers) {
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
     * 평가 결과 출력
     */
    protected void printResult(String configName, EvaluationResult result) {
        log.info(result.toString());
    }
}
