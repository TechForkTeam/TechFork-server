package com.techfork.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.llm.EmbeddingClient;
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
@Transactional
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingClient embeddingClient;
    private final GeneralSearchProperties generalSearchProperties;
    private final UserProfileDocumentRepository userProfileDocumentRepository;
    private final UserRepository userRepository;
    private final Executor searchAsyncExecutor;

    @Override
    public List<SearchResult> searchOnlyBm25(String query) {
        log.info("DEBUG MODE: Performing Lexical Search Only (BM25)");
        List<SearchResult> searchResults = searchOnlyBM25(query);
        log.info("Found {} results from lexical search.", searchResults.size());
        return searchResults.stream()
                .map(result -> result.toBuilder().titleVector(null).summaryVector(null).build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchResult> searchOnlySemantic(String query) {
        log.info("DEBUG MODE: Performing Semantic Search Only");
        List<SearchResult> searchResults = searchOnlySemantic(queryEmbedding(query));
        log.info("Found {} results from semantic search.", searchResults.size());
        return searchResults.stream()
                .map(result -> result.toBuilder().titleVector(null).summaryVector(null).build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchResult> searchGeneral(String query) {
        log.info("general search started: with query: '{}'", query);
        List<SearchResult> searchResults = performHybridSearch(query, queryEmbedding(query));
        log.info("Found {} results from hybrid search.", searchResults.size());
        return searchResults.stream()
                .map(result -> result.toBuilder().titleVector(null).summaryVector(null).build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchResult> searchPersonalized(String query, Long userId) {
        log.info("Personalized search started for userId: {} with query: '{}'", userId, query);
        List<Float> queryVector = queryEmbedding(query);
        List<SearchResult> initialResults = performHybridSearch(query, queryVector);
        log.info("Found {} initial results from hybrid search.", initialResults.size());

        User user = userRepository.getReferenceById(userId);

        log.info("Attempting to find user profile for userId: {}", user.getId());
        Optional<UserProfileDocument> userProfileOpt = userProfileDocumentRepository.findByUserId(user.getId());
        log.info("Successfully fetched user profile optional. isPresent: {}", userProfileOpt.isPresent());

        if (userProfileOpt.map(UserProfileDocument::getProfileVector).isEmpty()) {
            log.warn("User profile or vector not found for userId: {}. Returning non-personalized results.", user.getId());
            return initialResults.stream()
                    .map(result -> result.toBuilder().summaryVector(null).build())
                    .collect(Collectors.toList());
        }

        log.info("User profile and vector found. Proceeding to personal reranking.");
        float[] userProfileVector = userProfileOpt.get().getProfileVector();
        List<SearchResult> rerankedResults = personalReranking(initialResults, userProfileVector);
        log.info("Personal reranking complete. Returning {} results.", rerankedResults.size());
        return rerankedResults;
    }

    private List<Float> queryEmbedding(String query) {
        return embeddingClient.embed(query);
    }

    private List<SearchResult> performHybridSearch(String query, List<Float> queryVector) {
        CompletableFuture<List<Hit<PostDocument>>> lexicalFuture =
                CompletableFuture.supplyAsync(() -> performLexicalSearch(query), searchAsyncExecutor);
        CompletableFuture<List<Hit<PostDocument>>> semanticFuture =
                CompletableFuture.supplyAsync(() -> performSemanticSearch(queryVector), searchAsyncExecutor);

        CompletableFuture<List<SearchResult>> hybridResultFuture = lexicalFuture
                .thenCombine(semanticFuture, this::calculateRRF)
                .exceptionally(ex -> {
                    log.error("Hybrid search failed", ex);
                    throw new RuntimeException("통합 검색 중 오류 발생", ex);
                });

        return hybridResultFuture.join();
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

    private List<SearchResult> calculateRRF(List<Hit<PostDocument>> lexicalHits, List<Hit<PostDocument>> semanticHits) {
        Map<String, Integer> lexicalRankMap = new HashMap<>();
        AtomicInteger rank = new AtomicInteger(1);
        lexicalHits.forEach(hit -> lexicalRankMap.put(hit.id(), rank.getAndIncrement()));

        Map<String, Integer> semanticRankMap = new HashMap<>();
        rank.set(1);
        semanticHits.forEach(hit -> semanticRankMap.put(hit.id(), rank.getAndIncrement()));

        Map<String, SearchResult> combinedResults = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        processHitsForRRF(lexicalHits, lexicalRankMap, rrfScores, combinedResults);
        processHitsForRRF(semanticHits, semanticRankMap, rrfScores, combinedResults);

        return combinedResults.values().stream()
                .map(searchResult -> {
                    double finalScore = rrfScores.get(searchResult.getPostId().toString());
                    return searchResult.toBuilder()
                            .hybridScore(finalScore)
                            .finalScore(finalScore)
                            .build();
                })
                .sorted(Comparator.comparing(SearchResult::getFinalScore).reversed())
                .limit(generalSearchProperties.getSearchSize())
                .collect(Collectors.toList());
    }

    private void processHitsForRRF(List<Hit<PostDocument>> hits,
                                   Map<String, Integer> rankMap,
                                   Map<String, Double> rrfScores,
                                   Map<String, SearchResult> combinedResults) {
        hits.forEach(hit -> {
            String docId = hit.id();
            double score = 1.0 / (generalSearchProperties.getRRF_K() + rankMap.get(docId));
            rrfScores.merge(docId, score, Double::sum);

            SearchResult newResult = mapToSearchResult(hit);

            if (!combinedResults.containsKey(docId)) {
                combinedResults.put(docId, newResult);
            } else {
                SearchResult existing = combinedResults.get(docId);
                boolean needUpdate = false;
                SearchResult.SearchResultBuilder builder = existing.toBuilder();

                if (existing.getTitleVector() == null && newResult.getTitleVector() != null) {
                    builder.titleVector(newResult.getTitleVector());
                    needUpdate = true;
                }

                if (existing.getSummaryVector() == null && newResult.getSummaryVector() != null) {
                    builder.summaryVector(newResult.getSummaryVector());
                    needUpdate = true;
                }

                if (needUpdate) {
                    combinedResults.put(docId, builder.build());
                }
            }
        });
    }

    private List<Hit<PostDocument>> performLexicalSearch(String query) {
        String titleField = String.format(SearchConstants.TITLE_FIELD_FORMAT, generalSearchProperties.getTitleBoost());
        String summaryField = String.format(SearchConstants.SUMMARY_FIELD_FORMAT, generalSearchProperties.getSummaryBoost());

        Query lexicalQuery = Query.of(q -> q
                .bool(b -> b
                        .should(sh -> sh
                                .multiMatch(m -> m
                                        .query(query)
                                        .type(TextQueryType.MostFields)
                                        .fields(titleField, summaryField)
                                        .boost(generalSearchProperties.getExactBoost())
                                )
                        )
                        .should(sh -> sh
                                .multiMatch(m -> m
                                        .query(query)
                                        .fields(titleField, summaryField)
                                        .type(TextQueryType.MostFields)
                                        .fuzziness("AUTO")
                                        .prefixLength(1)
                                        .boost(generalSearchProperties.getFuzzyBoost())
                                )
                        )
                        .should(sh -> sh
                                .nested(n -> n
                                        .path(SearchConstants.CONTENT_CHUNKS_PATH)
                                        .query(nq -> nq
                                                .match(m -> m
                                                        .field(SearchConstants.CHUNK_TEXT_FIELD)
                                                        .query(query)
                                                )
                                        )
                                        .boost(generalSearchProperties.getChunkBoost())
                                )
                        )
                        .minimumShouldMatch(SearchConstants.MINIMUM_SHOULD_MATCH)
                )
        );

        try {
            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index(SearchConstants.POSTS_INDEX)
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

        knnSearches.add(KnnSearch.of(ks -> ks
                .field("titleEmbedding")
                .queryVector(queryVector)
                .k(k)
                .numCandidates(numCandidates)
                .boost(generalSearchProperties.getVectorTitleBoost())
        ));

        knnSearches.add(KnnSearch.of(ks -> ks
                .field("summaryEmbedding")
                .queryVector(queryVector)
                .k(k)
                .numCandidates(numCandidates)
                .boost(generalSearchProperties.getVectorSummaryBoost())
        ));

        knnSearches.add(KnnSearch.of(ks -> ks
                .field("contentChunks.embedding")
                .queryVector(queryVector)
                .k(k)
                .numCandidates(numCandidates)
                .boost(generalSearchProperties.getVectorContentChunkBoost())
        ));

        try {
            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index(SearchConstants.POSTS_INDEX)
                            .size(generalSearchProperties.getRRF_WINDOW_SIZE())
                            .knn(knnSearches),
                    PostDocument.class
            );

            return response.hits().hits();
        } catch (IOException e) {
            throw new RuntimeException("Semantic search failed", e);
        }
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
                .companyName(doc.getCompany())
                .url(doc.getUrl())
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
                .collect(Collectors.toList());
    }
}