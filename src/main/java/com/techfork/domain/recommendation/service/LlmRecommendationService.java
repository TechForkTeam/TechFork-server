package com.techfork.domain.recommendation.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.global.elasticsearch.query.VectorQueryBuilder;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.entity.RecommendationHistory;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.recommendation.repository.RecommendationHistoryRepository;
import com.techfork.domain.recommendation.service.MmrService.MmrCandidate;
import com.techfork.domain.recommendation.service.MmrService.MmrResult;
import com.techfork.domain.user.document.PersonalizationProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.PersonalizationProfileDocumentRepository;
import com.techfork.global.util.RrfScorer;
import com.techfork.global.util.TimeDecayStrategy;
import com.techfork.global.util.VectorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * MMR 알고리즘 기반 추천 전략 구현
 */
@Slf4j
@Service
@Primary
@Transactional
@RequiredArgsConstructor
public class LlmRecommendationService implements RecommendationService {

    private final ElasticsearchClient elasticsearchClient;
    private final PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;
    private final RecommendedPostRepository recommendedPostRepository;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    private final ReadPostRepository readPostRepository;
    private final PostRepository postRepository;
    private final MmrService mmrService;
    private final TimeDecayStrategy timeDecayStrategy;
    private final RecommendationProperties properties;
    private final VectorQueryBuilder vectorQueryBuilder;
    @Qualifier("recommendationAsyncExecutor")
    private final Executor recommendationAsyncExecutor;

    private static final String POSTS_INDEX = "posts";
    private static final String TITLE_EMBEDDING_FIELD = "titleEmbedding";
    private static final String SUMMARY_EMBEDDING_FIELD = "summaryEmbedding";
    private static final String CONTENT_CHUNKS_EMBEDDING_FIELD = "contentChunks.embedding";

    @Override
    public int generateRecommendationsForUser(User user) {
        log.info("사용자 {} 추천 생성 시작", user.getId());

        Optional<PersonalizationProfileDocument> personalizationProfileOpt =
                personalizationProfileDocumentRepository.findByUserId(user.getId());
        if (personalizationProfileOpt.isEmpty() || personalizationProfileOpt.get().getProfileVector() == null) {
            log.warn("사용자 {}의 개인화 프로필 또는 벡터를 찾을 수 없음. 추천 생성 스킵.", user.getId());
            return 0;
        }

        PersonalizationProfileDocument personalizationProfile = personalizationProfileOpt.get();
        float[] personalizationProfileVector = personalizationProfile.getProfileVector();

        try {
            // 2. k-NN 검색으로 초기 후보군 가져오기
            List<MmrCandidate> candidates = searchCandidates(personalizationProfileVector, user);

            if (candidates.isEmpty()) {
                log.info("사용자 {}의 추천 후보군을 찾을 수 없음", user.getId());
                return 0;
            }

            // 3. MMR 적용하여 최종 추천 선택
            List<MmrResult> mmrResults = mmrService.applyMmr(candidates);

            // 4. 기존 추천을 이력으로 보관
            List<RecommendedPost> oldRecommendations = recommendedPostRepository.findByUserOrderByRankAsc(user);
            if (!oldRecommendations.isEmpty()) {
                List<RecommendationHistory> histories = oldRecommendations.stream()
                        .map(RecommendationHistory::fromRecommendedPost)
                        .toList();
                recommendationHistoryRepository.saveAll(histories);
                recommendedPostRepository.deleteByUser(user);
            }

            // 5. 새 추천 저장
            List<RecommendedPost> recommendations = new ArrayList<>();
            for (MmrResult result : mmrResults) {
                Post post = postRepository.getReferenceById(result.getPostId());
                recommendations.add(RecommendedPost.create(
                        user, post, result.getSimilarityScore(), result.getMmrScore(), result.getRank()
                ));
            }

            recommendedPostRepository.saveAll(recommendations);
            return recommendations.size();

        } catch (Exception e) {
            log.error("사용자 {} 추천 생성 실패", user.getId(), e);
            throw new RuntimeException("추천 생성 중 오류가 발생했습니다.", e);
        }
    }

    private List<MmrCandidate> searchCandidates(float[] personalizationProfileVector, User user) throws IOException {
        Set<Long> readPostIds = readPostRepository.findRecentReadPostsByUserIdWithMinDuration(user.getId(), PageRequest.of(0, 1000))
                .stream()
                .map(readPost -> readPost.getPost().getId())
                .collect(Collectors.toSet());

        Optional<PersonalizationProfileDocument> personalizationProfileOpt =
                personalizationProfileDocumentRepository.findByUserId(user.getId());
        List<String> keyKeywords = personalizationProfileOpt
                .map(PersonalizationProfileDocument::getKeyKeywords)
                .orElse(List.of());

        RecommendationProperties.EmbeddingWeights weights = properties.getEmbeddingWeights();
        Query filterQuery = vectorQueryBuilder.createExcludeFilter(readPostIds);

        // 1. kNN 검색 쿼리 준비
        List<KnnSearch> knnSearches = vectorQueryBuilder.createKnnSearches(
                TITLE_EMBEDDING_FIELD, SUMMARY_EMBEDDING_FIELD, CONTENT_CHUNKS_EMBEDDING_FIELD,
                personalizationProfileVector, weights.getTitle(), weights.getSummary(), weights.getContent(),
                properties.getKnnSearchSize(), properties.getNumCandidates(), filterQuery
        );

        // 2. BM25 검색 쿼리 준비
        Query bm25Query = vectorQueryBuilder.createBm25Query(
                keyKeywords, weights.getTitle(), weights.getSummary(), weights.getContent()
        );

        long startTime = System.currentTimeMillis();

        // 3. kNN과 BM25 검색 병렬 실행
        CompletableFuture<List<Hit<PostDocument>>> vectorSearchFuture = CompletableFuture.supplyAsync(() -> {
            try {
                SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                                .index(POSTS_INDEX)
                                .knn(knnSearches)
                                .size(properties.getKnnSearchSize()),
                        PostDocument.class
                );
                return response.hits().hits();
            } catch (IOException e) {
                log.error("kNN 검색 실패", e);
                return Collections.emptyList();
            }
        }, recommendationAsyncExecutor);

        CompletableFuture<List<Hit<PostDocument>>> keywordSearchFuture = CompletableFuture.supplyAsync(() -> {
            // 키워드가 없으면 BM25 검색 생략
            if (bm25Query == null) {
                log.debug("키워드가 없어 BM25 검색 생략");
                return Collections.emptyList();
            }
            try {
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
                return response.hits().hits();
            } catch (IOException e) {
                log.error("BM25 검색 실패", e);
                return Collections.emptyList();
            }
        }, recommendationAsyncExecutor);

        // 4. 두 검색 완료 대기
        CompletableFuture<Void> allSearches = CompletableFuture.allOf(vectorSearchFuture, keywordSearchFuture);
        allSearches.join();

        List<Hit<PostDocument>> vectorHits = vectorSearchFuture.join();
        List<Hit<PostDocument>> keywordHits = keywordSearchFuture.join();

        log.info("후보군 검색 완료: kNN {} 개, BM25 {} 개, 소요 시간: {}ms",
                vectorHits.size(), keywordHits.size(), System.currentTimeMillis() - startTime);

        // 5. RRF로 결합
        return applyRrf(vectorHits, keywordHits);
    }

    protected List<MmrCandidate> applyRrf(List<Hit<PostDocument>> vectorHits, List<Hit<PostDocument>> keywordHits) {
        // Post ID 리스트 추출 (null 체크)
        List<Long> vectorPostIds = vectorHits.stream()
                .filter(hit -> hit.source() != null)
                .map(hit -> hit.source().getPostId())
                .toList();

        List<Long> keywordPostIds = keywordHits.stream()
                .filter(hit -> hit.source() != null)
                .map(hit -> hit.source().getPostId())
                .toList();

        // RRF 스코어 계산
        Map<Long, Double> rrfScores = RrfScorer.calculateRrfScores(vectorPostIds, keywordPostIds);

        // Hit을 postId 기준으로 맵핑
        Map<Long, Hit<PostDocument>> hitMap = new HashMap<>();
        vectorHits.stream()
                .filter(hit -> hit.source() != null)
                .forEach(hit -> hitMap.putIfAbsent(hit.source().getPostId(), hit));
        keywordHits.stream()
                .filter(hit -> hit.source() != null)
                .forEach(hit -> hitMap.putIfAbsent(hit.source().getPostId(), hit));

        // RRF 스코어 순으로 정렬하여 MMR Candidate 생성
        // MMR 성능을 위해 상위 N개만 선택 (MMR은 O(n²)이므로 후보 수 제한 필요)
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(properties.getMmrCandidateSize())
                .map(entry -> mapToMmrCandidate(hitMap.get(entry.getKey()), entry.getValue()))
                .filter(candidate -> candidate.getSummaryVector() != null)
                .toList();
    }

    protected MmrCandidate mapToMmrCandidate(Hit<PostDocument> hit, double rrfScore) {
        PostDocument doc = hit.source();
        double timeDecayWeight = timeDecayStrategy.calculateWeight(Objects.requireNonNull(doc).getPublishedAt());

        return MmrCandidate.builder()
                .postId(doc.getPostId())
                .titleVector(VectorUtil.convertToFloatArray(doc.getTitleEmbedding()))
                .summaryVector(VectorUtil.convertToFloatArray(doc.getSummaryEmbedding()))
                .similarityScore(rrfScore * timeDecayWeight)
                .build();
    }
}
