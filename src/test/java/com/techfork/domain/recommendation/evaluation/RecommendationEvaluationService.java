package com.techfork.domain.recommendation.evaluation;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.repository.RecommendationHistoryRepository;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.recommendation.service.LlmRecommendationService;
import com.techfork.domain.recommendation.service.MmrService;
import com.techfork.domain.recommendation.service.MmrService.MmrCandidate;
import com.techfork.domain.recommendation.service.MmrService.MmrResult;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.global.elasticsearch.query.VectorQueryBuilder;
import com.techfork.global.util.TimeDecayStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 추천 시스템 성능 평가를 위한 전용 서비스
 * LlmRecommendationService를 상속하여 RRF, MMR 로직 재사용
 */
@Slf4j
@Service
public class RecommendationEvaluationService extends LlmRecommendationService {

    private final UserProfileDocumentRepository userProfileDocumentRepository;
    private final VectorQueryBuilder vectorQueryBuilder;
    private final ElasticsearchClient elasticsearchClient;

    private static final String POSTS_INDEX = "posts";
    private static final String TITLE_EMBEDDING_FIELD = "titleEmbedding";
    private static final String SUMMARY_EMBEDDING_FIELD = "summaryEmbedding";
    private static final String CONTENT_CHUNKS_EMBEDDING_FIELD = "contentChunks.embedding";

    public RecommendationEvaluationService(
            ElasticsearchClient elasticsearchClient,
            UserProfileDocumentRepository userProfileDocumentRepository,
            RecommendedPostRepository recommendedPostRepository,
            RecommendationHistoryRepository recommendationHistoryRepository,
            ReadPostRepository readPostRepository,
            PostRepository postRepository,
            MmrService mmrService,
            TimeDecayStrategy timeDecayStrategy,
            RecommendationProperties properties,
            VectorQueryBuilder vectorQueryBuilder
    ) {
        super(elasticsearchClient, userProfileDocumentRepository, recommendedPostRepository,
                recommendationHistoryRepository, readPostRepository, postRepository,
                mmrService, timeDecayStrategy, properties, vectorQueryBuilder,
                Executors.newSingleThreadExecutor());
        this.elasticsearchClient = elasticsearchClient;
        this.userProfileDocumentRepository = userProfileDocumentRepository;
        this.vectorQueryBuilder = vectorQueryBuilder;
    }

    /**
     * 추천 생성 (평가 전용 - Train/Test Split 지원)
     */
    public List<Long> generateRecommendationsForEvaluation(User user, Set<Long> trainPostIds, RecommendationProperties properties) {
        long totalStartTime = System.currentTimeMillis();

        Optional<UserProfileDocument> profileOpt = userProfileDocumentRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty() || profileOpt.get().getProfileVector() == null) {
            return Collections.emptyList();
        }

        UserProfileDocument profile = profileOpt.get();
        float[] userProfileVector = profile.getProfileVector();
        List<String> keyKeywords = profile.getKeyKeywords();

        try {
            List<MmrCandidate> candidates = searchCandidatesWithCustomReadHistory(userProfileVector, keyKeywords, trainPostIds, properties);

            if (candidates.isEmpty()) {
                return Collections.emptyList();
            }

            // MMR 적용 (테스트용 properties 사용)
            long mmrStartTime = System.currentTimeMillis();
            MmrService mmrService = new MmrService(properties);
            List<MmrResult> mmrResults = mmrService.applyMmr(candidates);
            long mmrElapsedTime = System.currentTimeMillis() - mmrStartTime;
            log.info("[EVAL] MMR 실행 시간: {}ms (후보 {}개 → 결과 {}개)", mmrElapsedTime, candidates.size(), mmrResults.size());

            List<Long> result = mmrResults.stream()
                    .map(MmrResult::getPostId)
                    .toList();

            long totalElapsedTime = System.currentTimeMillis() - totalStartTime;
            log.info("[EVAL] 전체 추천 로직 실행 시간: {}ms (사용자 ID: {})", totalElapsedTime, user.getId());

            return result;

        } catch (Exception e) {
            log.error("사용자 {} 평가용 추천 생성 실패", user.getId(), e);
            return Collections.emptyList();
        }
    }

    private List<MmrCandidate> searchCandidatesWithCustomReadHistory(
            float[] userProfileVector,
            List<String> keyKeywords,
            Set<Long> readPostIds,
            RecommendationProperties properties) throws IOException {

        RecommendationProperties.EmbeddingWeights weights = properties.getEmbeddingWeights();
        Query filterQuery = vectorQueryBuilder.createExcludeFilter(readPostIds);

        // 1. kNN 검색 쿼리 준비
        List<KnnSearch> knnSearches = vectorQueryBuilder.createKnnSearches(
                TITLE_EMBEDDING_FIELD, SUMMARY_EMBEDDING_FIELD, CONTENT_CHUNKS_EMBEDDING_FIELD,
                userProfileVector, weights.getTitle(), weights.getSummary(), weights.getContent(),
                properties.getKnnSearchSize(), properties.getNumCandidates(), filterQuery
        );

        // 2. BM25 검색 쿼리 준비
        Query bm25Query = vectorQueryBuilder.createBm25Query(
                keyKeywords, weights.getTitle(), weights.getSummary(), weights.getContent()
        );

        // 3. kNN과 BM25 검색 병렬 실행
        long searchStartTime = System.currentTimeMillis();

        CompletableFuture<List<Hit<PostDocument>>> vectorSearchFuture = CompletableFuture.supplyAsync(() -> {
            try {
                long knnStartTime = System.currentTimeMillis();
                SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                                .index(POSTS_INDEX)
                                .knn(knnSearches)
                                .size(properties.getKnnSearchSize()),
                        PostDocument.class
                );
                long knnElapsedTime = System.currentTimeMillis() - knnStartTime;
                log.info("[EVAL] kNN 검색 실행 시간: {}ms", knnElapsedTime);
                return response.hits().hits();
            } catch (IOException e) {
                log.error("kNN 검색 실패", e);
                return Collections.emptyList();
            }
        });

        CompletableFuture<List<Hit<PostDocument>>> keywordSearchFuture = CompletableFuture.supplyAsync(() -> {
            // 키워드가 없으면 BM25 검색 생략
            if (bm25Query == null) {
                log.debug("[EVAL] 키워드가 없어 BM25 검색 생략");
                return Collections.emptyList();
            }
            try {
                long bm25StartTime = System.currentTimeMillis();
                SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                                .index(POSTS_INDEX)
                                .query(q -> q.bool(b -> {
                                    b.must(bm25Query);
                                    if (filterQuery != null) b.filter(filterQuery);
                                    return b;
                                }))
                                .size(properties.getKnnSearchSize()),
                        PostDocument.class
                );
                long bm25ElapsedTime = System.currentTimeMillis() - bm25StartTime;
                log.info("[EVAL] BM25 검색 실행 시간: {}ms", bm25ElapsedTime);
                return response.hits().hits();
            } catch (IOException e) {
                log.error("BM25 검색 실패", e);
                return Collections.emptyList();
            }
        });

        // 4. 두 검색 완료 대기
        CompletableFuture<Void> allSearches = CompletableFuture.allOf(vectorSearchFuture, keywordSearchFuture);
        allSearches.join();

        List<Hit<PostDocument>> vectorHits = vectorSearchFuture.join();
        List<Hit<PostDocument>> keywordHits = keywordSearchFuture.join();

        long searchTotalTime = System.currentTimeMillis() - searchStartTime;
        log.info("[EVAL] 검색 총 소요 시간: {}ms (kNN: {}개, BM25: {}개)", searchTotalTime, vectorHits.size(), keywordHits.size());

        // 5. RRF로 결합 (부모 클래스의 protected 메서드 사용)
        long rrfStartTime = System.currentTimeMillis();
        List<MmrCandidate> candidates = applyRrf(vectorHits, keywordHits);
        long rrfElapsedTime = System.currentTimeMillis() - rrfStartTime;
        log.info("[EVAL] RRF 결합 실행 시간: {}ms (결과: {}개)", rrfElapsedTime, candidates.size());

        return candidates;
    }
}