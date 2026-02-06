package com.techfork.domain.recommendation.evaluation;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.service.MmrService;
import com.techfork.domain.recommendation.service.MmrService.MmrCandidate;
import com.techfork.domain.recommendation.service.MmrService.MmrResult;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.global.elasticsearch.query.VectorQueryBuilder;
import com.techfork.global.util.TimeDecayStrategy;
import com.techfork.global.util.VectorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 추천 시스템 성능 평가를 위한 전용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationEvaluationService {

    private final ElasticsearchClient elasticsearchClient;
    private final UserProfileDocumentRepository userProfileDocumentRepository;
    private final VectorQueryBuilder vectorQueryBuilder;
    private final TimeDecayStrategy timeDecayStrategy;

    private static final String POSTS_INDEX = "posts";
    private static final String TITLE_EMBEDDING_FIELD = "titleEmbedding";
    private static final String SUMMARY_EMBEDDING_FIELD = "summaryEmbedding";
    private static final String CONTENT_CHUNKS_EMBEDDING_FIELD = "contentChunks.embedding";

    /**
     * 추천 생성 (평가 전용 - Train/Test Split 지원)
     */
    public List<Long> generateRecommendationsForEvaluation(User user, Set<Long> trainPostIds, RecommendationProperties properties) {
        Optional<UserProfileDocument> profileOpt = userProfileDocumentRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty() || profileOpt.get().getProfileVector() == null) {
            return Collections.emptyList();
        }

        float[] userProfileVector = profileOpt.get().getProfileVector();
        List<String> keywords = profileOpt.get().getInterests();

        try {
            List<MmrCandidate> candidates = searchCandidatesWithCustomReadHistory(userProfileVector, keywords, user, trainPostIds, properties);

            if (candidates.isEmpty()) {
                return Collections.emptyList();
            }

            // MMR 적용 (테스트용 properties 사용)
            MmrService mmrService = new MmrService(properties);
            List<MmrResult> mmrResults = mmrService.applyMmr(candidates);

            return mmrResults.stream()
                    .map(MmrResult::getPostId)
                    .toList();

        } catch (Exception e) {
            log.error("사용자 {} 평가용 추천 생성 실패", user.getId(), e);
            return Collections.emptyList();
        }
    }

    private List<MmrCandidate> searchCandidatesWithCustomReadHistory(
            float[] userProfileVector,
            List<String> keywords,
            User user,
            Set<Long> readPostIds,
            RecommendationProperties properties) throws IOException {

        RecommendationProperties.EmbeddingWeights weights = properties.getEmbeddingWeights();
        Query filterQuery = vectorQueryBuilder.createExcludeFilter(readPostIds);

        List<KnnSearch> knnSearches = vectorQueryBuilder.createKnnSearches(
                TITLE_EMBEDDING_FIELD, SUMMARY_EMBEDDING_FIELD, CONTENT_CHUNKS_EMBEDDING_FIELD,
                userProfileVector, weights.getTitle(), weights.getSummary(), weights.getContent(),
                properties.getKnnSearchSize(), properties.getNumCandidates(), filterQuery
        );

        SearchResponse<PostDocument> vectorResponse = elasticsearchClient.search(s -> s
                        .index(POSTS_INDEX).knn(knnSearches).size(properties.getKnnSearchSize()),
                PostDocument.class
        );

        return vectorResponse.hits().hits().stream()
                .filter(hit -> hit.source() != null)
                .map(hit -> mapToMmrCandidate(hit, hit.score() != null ? hit.score() : 0.0))
                .filter(candidate -> candidate.getSummaryVector() != null)
                .toList();
    }

    private MmrCandidate mapToMmrCandidate(Hit<PostDocument> hit, double score) {
        PostDocument doc = hit.source();
        double timeDecayWeight = timeDecayStrategy.calculateWeight(Objects.requireNonNull(doc).getPublishedAt());
        return MmrCandidate.builder()
                .postId(doc.getPostId())
                .titleVector(VectorUtil.convertToFloatArray(doc.getTitleEmbedding()))
                .summaryVector(VectorUtil.convertToFloatArray(doc.getSummaryEmbedding()))
                .similarityScore(score * timeDecayWeight)
                .build();
    }
}