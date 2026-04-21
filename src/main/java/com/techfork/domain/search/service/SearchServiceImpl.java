package com.techfork.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.techfork.domain.activity.repository.BookmarkRepository;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.search.config.GeneralSearchProperties;
import com.techfork.domain.search.dto.SearchResult;

import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import com.techfork.global.util.RrfScorer;
import com.techfork.global.util.VectorUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final String POSTS_INDEX = "posts";
    private static final String TITLE_FIELD_FORMAT = "title^%.1f";
    private static final String SUMMARY_FIELD_FORMAT = "summary^%.1f";
    private static final String CONTENT_CHUNKS_PATH = "contentChunks";
    private static final String CHUNK_TEXT_FIELD = "contentChunks.chunkText";
    private static final String MINIMUM_SHOULD_MATCH = "1";

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingClient embeddingClient;
    private final GeneralSearchProperties generalSearchProperties;
    private final UserProfileDocumentRepository userProfileDocumentRepository;
    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;
    private final Executor searchAsyncExecutor;
    private final CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    @Override
    public List<SearchResult> searchOnlyBm25(String query) {
        List<SearchResult> searchResults = searchOnlyBM25(query);
        return stripVectors(searchResults);
    }

    @Override
    public List<SearchResult> searchOnlySemantic(String query) {
        List<SearchResult> searchResults = searchOnlySemantic(queryEmbedding(query));
        return stripVectors(searchResults);
    }

    @Override
    public List<SearchResult> searchGeneral(String query) {
        log.debug("general search started: with query: '{}'", query);
        long startTime = System.currentTimeMillis();

        List<SearchResult> searchResults = performHybridSearch(query, generalSearchProperties.getSearchSize());

        searchResults = attachPostMetadata(searchResults, null);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Search completed. Query='{}', Results={}, Time={}ms", query, searchResults.size(), duration);

        return stripVectors(searchResults);
    }

    @Override
    public List<SearchResult> searchPersonalized(String query, Long userId) {
        log.debug("Personalized search started for userId: {} with query: '{}'", userId, query);
        long startTime = System.currentTimeMillis();

        Optional<UserProfileDocument> userProfileOpt = userProfileDocumentRepository.findByUserId(userId);
        boolean hasProfile = userProfileOpt.isPresent() && userProfileOpt.get().getProfileVector() != null;

        int candidateSize = hasProfile
                ? generalSearchProperties.getRRF_WINDOW_SIZE()
                : generalSearchProperties.getSearchSize();

        List<SearchResult> initialResults = performHybridSearch(query, candidateSize);
        log.debug("Initial hybrid search found {} documents.", initialResults.size());

        List<SearchResult> finalResults;
        if (!hasProfile) {
            finalResults = initialResults;
            long duration = System.currentTimeMillis() - startTime;
            log.info("Personalized Search [FALLBACK]. UserID={}, Query='{}', Results={}, Time={}ms (Reason: No Profile)",
                    userId, query, finalResults.size(), duration);
        } else {
            float[] userProfileVector = userProfileOpt.get().getProfileVector();
            finalResults = personalReranking(initialResults, userProfileVector);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Personalized Search [RERANKED]. UserID={}, Query='{}', Results={}, Time={}ms",
                    userId, query, finalResults.size(), duration);
        }

        finalResults = attachPostMetadata(finalResults, userId);

        return stripVectors(finalResults);
    }

    private List<SearchResult> searchOnlyBM25(String query) {
        log.info("DEBUG MODE: Performing Lexical Search Only (BM25)");

        List<Hit<PostDocument>> lexicalHits = performLexicalSearch(query);

        return lexicalHits.stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());
    }

    private List<SearchResult> searchOnlySemantic(List<Float> queryVector) {
        log.info("DEBUG MODE: Performing Semantic Search Only");

        List<Hit<PostDocument>> semanticHits = performSemanticSearch(queryVector);

        return semanticHits.stream()
                .map(this::mapToSearchResult)
                .toList();
    }

    private List<Float> queryEmbedding(String query) {
        return embeddingClient.embed(query);
    }

    private List<SearchResult> performHybridSearch(String query, int candidateSize) {
        // BM25: 임베딩 없이 즉시 시작
        CompletableFuture<List<Hit<PostDocument>>> lexicalFuture = CompletableFuture.supplyAsync(() -> {
                    long t = System.currentTimeMillis();
                    var result = performLexicalSearch(query);
                    log.debug("Lexical search done. Hits={}, Time={}ms", result.size(), System.currentTimeMillis() - t);
                    return result;
                }, searchAsyncExecutor);

        // 임베딩 → KNN: BM25와 동시에 시작
        CompletableFuture<List<Hit<PostDocument>>> semanticFuture = CompletableFuture.supplyAsync(() -> {
                    long t = System.currentTimeMillis();
                    List<Float> queryVector = queryEmbedding(query);
                    log.debug("Embedding done. Time={}ms", System.currentTimeMillis() - t);
                    long t2 = System.currentTimeMillis();
                    var result = performSemanticSearch(queryVector);
                    log.debug("Semantic search done. Hits={}, Time={}ms", result.size(), System.currentTimeMillis() - t2);
                    return result;
                }, searchAsyncExecutor);

        return lexicalFuture
                .thenCombine(semanticFuture, (lexicalHits, semanticHits) -> {
                    log.debug("Merging results: Lexical Hits={}, Semantic Hits={}", lexicalHits.size(), semanticHits.size());
                    return calculateRRF(lexicalHits, semanticHits, candidateSize);
                })
                .exceptionally(ex -> {
                    log.error("Hybrid search failed for query: '{}'", query, ex);
                    throw new RuntimeException("통합 검색 중 오류 발생", ex);
                })
                .join();
    }

    private List<Hit<PostDocument>> performLexicalSearch(String query) {
        String titleField = String.format(TITLE_FIELD_FORMAT, generalSearchProperties.getTitleBoost());
        String summaryField = String.format(SUMMARY_FIELD_FORMAT, generalSearchProperties.getSummaryBoost());
        boolean useBm25Chunk = generalSearchProperties.getBm25ChunkBoost() > 0.0f;

        Query titleSummaryQuery = Query.of(q -> q
                .disMax(dm -> dm
                        .queries(
                                Query.of(inner -> inner
                                        .multiMatch(m -> m
                                                .query(query)
                                                .type(TextQueryType.MostFields)
                                                .fields(titleField, summaryField)
                                                .boost(generalSearchProperties.getExactBoost())
                                        )
                                ),
                                Query.of(inner -> inner
                                        .multiMatch(m -> m
                                                .query(query)
                                                .fields(titleField, summaryField)
                                                .type(TextQueryType.MostFields)
                                                .fuzziness("AUTO")
                                                .prefixLength(1)
                                                .boost(generalSearchProperties.getFuzzyBoost())
                                        )
                                )
                        )
                        .tieBreaker((double) generalSearchProperties.getTieBreaker())
                )
        );

        Query lexicalQuery = Query.of(q -> q
                .bool(b -> {
                    b.should(titleSummaryQuery)
                    .minimumShouldMatch(MINIMUM_SHOULD_MATCH);
                    if (useBm25Chunk) {
                        b.should(sh -> sh
                                .nested(n -> n
                                        .path(CONTENT_CHUNKS_PATH)
                                        .query(nq -> nq
                                                .match(m -> m
                                                        .field(CHUNK_TEXT_FIELD)
                                                        .query(query)
                                                )
                                        )
                                        .boost(generalSearchProperties.getBm25ChunkBoost())
                                )
                        );
                    }
                    return b;
                })
        );

        try {
            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index(POSTS_INDEX)
                            .size(generalSearchProperties.getRRF_WINDOW_SIZE())
                            .query(lexicalQuery),
                    PostDocument.class
            );
            return response.hits().hits();
        } catch (IOException e) {
            throw new RuntimeException("Lexical search failed", e);
        }
    }

    private List<Hit<PostDocument>> performSemanticSearch(List<Float> queryVector) {
        int k = generalSearchProperties.getKnnK();
        int numCandidates = generalSearchProperties.getKnnNumCandidates();

        List<KnnSearch> knnSearches = new ArrayList<>();
        knnSearches.add(createKnnSearch("titleEmbedding", queryVector, k, numCandidates, generalSearchProperties.getTitleBoost()));
        knnSearches.add(createKnnSearch("summaryEmbedding", queryVector, k, numCandidates, generalSearchProperties.getSummaryBoost()));
        if (generalSearchProperties.getVectorChunkBoost() > 0.0f) {
            knnSearches.add(createKnnSearch("contentChunks.embedding", queryVector, k, numCandidates, generalSearchProperties.getVectorChunkBoost()));
        }

        try {
            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index(POSTS_INDEX)
                            .size(generalSearchProperties.getRRF_WINDOW_SIZE())
                            .knn(knnSearches),
                    PostDocument.class
            );

            return response.hits().hits();
        } catch (IOException e) {
            throw new RuntimeException("Semantic search failed", e);
        }
    }

    private KnnSearch createKnnSearch(String field, List<Float> vector, int k, int numCandidates, float boost) {
        return KnnSearch.of(ks -> ks
                .field(field)
                .queryVector(vector)
                .k(k)
                .numCandidates(numCandidates)
                .boost(boost)
        );
    }

    private List<SearchResult> calculateRRF(List<Hit<PostDocument>> lexicalHits, List<Hit<PostDocument>> semanticHits, int limit) {
        // Hit ID 리스트 추출
        List<String> lexicalIds = lexicalHits.stream().map(Hit::id).toList();
        List<String> semanticIds = semanticHits.stream().map(Hit::id).toList();

        // RRF 스코어 계산
        Map<String, Double> rrfScores = RrfScorer.calculateRrfScores(lexicalIds, semanticIds);

        // Hit을 docId 기준으로 맵핑 (semantic 우선 - 벡터 포함 보장)
        Map<String, Hit<PostDocument>> hitMap = new HashMap<>();
        lexicalHits.forEach(hit -> hitMap.put(hit.id(), hit));
        semanticHits.forEach(hit -> hitMap.put(hit.id(), hit)); // semantic 결과로 덮어쓰기 (벡터 포함)

        // SearchResult로 변환
        Map<String, SearchResult> resultMap = new HashMap<>();
        for (Map.Entry<String, Hit<PostDocument>> entry : hitMap.entrySet()) {
            String docId = entry.getKey();
            Hit<PostDocument> hit = entry.getValue();
            SearchResult result = mapToSearchResult(hit);
            resultMap.put(docId, result);
        }

        // 최종 스코어 적용 및 정렬
        return resultMap.values().stream()
                .map(searchResult -> {
                    double finalScore = rrfScores.get(searchResult.getPostId().toString());
                    return searchResult.toBuilder()
                            .hybridScore(finalScore)
                            .finalScore(finalScore)
                            .build();
                })
                .sorted(Comparator.comparing(SearchResult::getFinalScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private SearchResult mapToSearchResult(Hit<PostDocument> hit) {
        PostDocument doc = hit.source();
        double score = Objects.requireNonNullElse(hit.score(), 0.0);

        float[] titleVector = VectorUtil.convertToFloatArray(Objects.requireNonNull(doc).getTitleEmbedding());
        float[] summaryVector = VectorUtil.convertToFloatArray(Objects.requireNonNull(doc).getSummaryEmbedding());

        return SearchResult.builder()
                .postId(doc.getPostId())
                .title(doc.getTitle())
                .summary(doc.getSummary())
                .shortSummary(doc.getShortSummary())
                .companyName(doc.getCompany())
                .url(doc.getUrl())
                .logoUrl(doc.getLogoUrl())
                .thumbnailUrl(thumbnailOptimizer.optimize(doc.getThumbnailUrl()))
                .publishedAt(doc.getPublishedAt())
                .hybridScore(score)
                .finalScore(score)
                .titleVector(titleVector)
                .summaryVector(summaryVector)
                .build();
    }

    private List<SearchResult> personalReranking(List<SearchResult> initialResults, float[] userProfileVector) {
        return initialResults.stream()
                .map(result -> {
                    double titleSim = 0.0;
                    if (result.getTitleVector() != null && result.getTitleVector().length > 0) {
                        titleSim = VectorUtil.cosineSimilarity(userProfileVector, result.getTitleVector());
                    }

                    double summarySim = 0.0;
                    if (result.getSummaryVector() != null) {
                        summarySim = VectorUtil.cosineSimilarity(userProfileVector, result.getSummaryVector());
                    }

                    double personalScore = (titleSim * generalSearchProperties.getRerankDocumentTitleWeight()) + (summarySim * generalSearchProperties.getRerankDocumentSummaryWeight());

                    double finalScore = (result.getHybridScore() * generalSearchProperties.getHybridScoreWeight())
                            + (personalScore * generalSearchProperties.getPersonalScoreWeight());

                    return result.toBuilder()
                            .personalScore(personalScore)
                            .finalScore(finalScore)
                            .titleVector(null)
                            .summaryVector(null)
                            .build();
                })
                .sorted(Comparator.comparing(SearchResult::getFinalScore).reversed())
                .limit(generalSearchProperties.getSearchSize())
                .collect(Collectors.toList());
    }

    private List<SearchResult> stripVectors(List<SearchResult> results) {
        return results.stream()
                .map(result -> result.toBuilder()
                        .titleVector(null)
                        .summaryVector(null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<SearchResult> attachPostMetadata(List<SearchResult> results, Long userId) {
        if (results.isEmpty()) {
            return results;
        }

        // Collect postIds
        List<Long> postIds = results.stream()
                .map(SearchResult::getPostId)
                .collect(Collectors.toList());

        // Fetch viewCount from MySQL (in batch)
        Map<Long, Long> viewCountMap = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Post::getViewCount));

        // Fetch bookmark status if userId is provided
        List<Long> bookmarkedPostIds = userId != null
                ? bookmarkRepository.findBookmarkedPostIds(userId, postIds)
                : List.of();

        // Attach metadata to results
        return results.stream()
                .map(result -> result.toBuilder()
                        .viewCount(viewCountMap.getOrDefault(result.getPostId(), 0L))
                        .isBookmarked(userId != null && bookmarkedPostIds.contains(result.getPostId()))
                        .build())
                .collect(Collectors.toList());
    }
}
