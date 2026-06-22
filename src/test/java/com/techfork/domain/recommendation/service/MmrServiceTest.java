package com.techfork.domain.recommendation.service;

import com.techfork.domain.recommendation.config.RecommendationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("MmrService 단위 테스트")
class MmrServiceTest {

    @Test
    @DisplayName("후보군이 null 또는 비어 있으면 빈 결과를 반환한다")
    void applyMmr_NullOrEmptyCandidates_ReturnsEmptyList() {
        MmrService mmrService = new MmrService(deterministicProperties(3, 0.95));

        assertThat(mmrService.applyMmr(null)).isEmpty();
        assertThat(mmrService.applyMmr(List.of())).isEmpty();
    }

    @Test
    @DisplayName("최종 추천 수는 설정된 finalSize와 후보 수 중 작은 값으로 제한된다")
    void applyMmr_LimitsResultSizeByConfiguredFinalSize() {
        MmrService mmrService = new MmrService(deterministicProperties(2, 1.0));

        List<MmrService.MmrResult> results = mmrService.applyMmr(List.of(
                candidate(1L, 0.9, vector(1, 0), vector(1, 0)),
                candidate(2L, 0.8, vector(0, 1), vector(0, 1)),
                candidate(3L, 0.7, vector(-1, 0), vector(-1, 0))
        ));

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(MmrService.MmrResult::getRank)
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("Top-K 샘플링을 1로 설정하면 관련성 점수 순위가 결정적으로 재현된다")
    void applyMmr_WithTopKOne_ReturnsDeterministicRanking() {
        MmrService mmrService = new MmrService(deterministicProperties(3, 1.0));

        List<MmrService.MmrResult> results = mmrService.applyMmr(List.of(
                candidate(1L, 0.9, vector(1, 0), vector(1, 0)),
                candidate(2L, 0.8, vector(0, 1), vector(0, 1)),
                candidate(3L, 0.7, vector(-1, 0), vector(-1, 0))
        ));

        assertThat(results)
                .extracting(MmrService.MmrResult::getPostId)
                .containsExactly(1L, 2L, 3L);
        assertThat(results)
                .extracting(MmrService.MmrResult::getSimilarityScore)
                .containsExactly(0.9, 0.8, 0.7);
    }

    @Test
    @DisplayName("lambda가 1이면 다양성 페널티 없이 관련성 점수만으로 후보를 선택한다")
    void applyMmr_WithLambdaOne_PrioritizesRelevanceOnly() {
        MmrService mmrService = new MmrService(deterministicProperties(3, 1.0));

        List<MmrService.MmrResult> results = mmrService.applyMmr(List.of(
                candidate(1L, 0.9, vector(1, 0), vector(1, 0)),
                candidate(2L, 0.85, vector(1, 0), vector(1, 0)),
                candidate(3L, 0.8, vector(-1, 0), vector(-1, 0))
        ));

        assertThat(results)
                .extracting(MmrService.MmrResult::getPostId)
                .containsExactly(1L, 2L, 3L);
        assertThat(results.get(1).getMmrScore()).isCloseTo(0.85, within(1.0e-9));
        assertThat(results.get(2).getMmrScore()).isCloseTo(0.8, within(1.0e-9));
    }

    @Test
    @DisplayName("lambda가 낮으면 이미 선택된 문서와 유사한 후보에 diversity penalty를 적용한다")
    void applyMmr_WithLowLambda_AppliesDiversityPenalty() {
        MmrService mmrService = new MmrService(deterministicProperties(2, 0.5));

        List<MmrService.MmrResult> results = mmrService.applyMmr(List.of(
                candidate(1L, 1.0, vector(1, 0), vector(1, 0)),
                candidate(2L, 0.95, vector(1, 0), vector(1, 0)),
                candidate(3L, 0.8, vector(-1, 0), vector(-1, 0))
        ));

        assertThat(results)
                .extracting(MmrService.MmrResult::getPostId)
                .containsExactly(1L, 3L);
        assertThat(results.get(1).getMmrScore()).isCloseTo(0.4, within(1.0e-9));
    }

    private RecommendationProperties deterministicProperties(int finalSize, double lambda) {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setMmrFinalSize(finalSize);
        properties.setLambda(lambda);
        properties.setMmrFirstTopK(1);
        properties.setMmrTopK(1);
        properties.setEmbeddingWeights(new RecommendationProperties.EmbeddingWeights(0.5f, 0.5f, 0.0f));
        return properties;
    }

    private MmrService.MmrCandidate candidate(Long postId, double similarityScore, float[] titleVector, float[] summaryVector) {
        return MmrService.MmrCandidate.builder()
                .postId(postId)
                .similarityScore(similarityScore)
                .titleVector(titleVector)
                .summaryVector(summaryVector)
                .build();
    }

    private float[] vector(float x, float y) {
        return new float[]{x, y};
    }
}
