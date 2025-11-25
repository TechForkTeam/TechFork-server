package com.techfork.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.util.VectorUtil;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private static final class SearchConstants {
        static final String POSTS_INDEX = "posts";
        static final String TITLE_FIELD_FORMAT = "title^%.1f";
        static final String SUMMARY_FIELD_FORMAT = "summary^%.1f";
        static final String CONTENT_CHUNKS_PATH = "contentChunks";
        static final String CHUNK_TEXT_FIELD = "contentChunks.chunkText";
        static final String MINIMUM_SHOULD_MATCH = "0";
        static final String SCRIPT_SOURCE_SEMANTIC = "cosineSimilarity(params.query_vector, 'summaryEmbedding') + 1.0";
        static final String QUERY_VECTOR_PARAM = "query_vector";
        static final int RRF_K = 60;
    }

    @Override
    public List<SearchResult> searchGeneral(String query) {
        log.info("general search started: with query: '{}'", query);
        List<Float> queryVector = queryEmbedding(query);
        List<SearchResult> searchResults = performHybridSearch(query, queryVector);
        log.info("Found {} results from hybrid search.", searchResults.size());
        return searchResults.stream()
                .map(result -> result.toBuilder().documentVector(null).build())
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
                    .map(result -> result.toBuilder().documentVector(null).build())
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
        List<Hit<PostDocument>> lexicalHits = performLexicalSearch(query);
        List<Hit<PostDocument>> semanticHits = performSemanticSearch(queryVector);

        Map<String, Integer> lexicalRankMap = new HashMap<>();
        AtomicInteger rank = new AtomicInteger(1);
        lexicalHits.forEach(hit -> lexicalRankMap.put(hit.id(), rank.getAndIncrement()));

        Map<String, Integer> semanticRankMap = new HashMap<>();
        rank.set(1);
        semanticHits.forEach(hit -> semanticRankMap.put(hit.id(), rank.getAndIncrement()));

        Map<String, SearchResult> combinedResults = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        lexicalHits.forEach(hit -> {
            String docId = hit.id();
            double score = 1.0 / (SearchConstants.RRF_K + lexicalRankMap.get(docId));
            rrfScores.put(docId, rrfScores.getOrDefault(docId, 0.0) + score);
            if (!combinedResults.containsKey(docId)) {
                combinedResults.put(docId, mapToSearchResult(hit));
            }
        });

        semanticHits.forEach(hit -> {
            String docId = hit.id();
            double score = 1.0 / (SearchConstants.RRF_K + semanticRankMap.get(docId));
            rrfScores.put(docId, rrfScores.getOrDefault(docId, 0.0) + score);
            if (!combinedResults.containsKey(docId)) {
                combinedResults.put(docId, mapToSearchResult(hit));
            }
        });

        return combinedResults.values().stream()
                .map(searchResult -> {
                    double finalScore = rrfScores.get(searchResult.getPostId().toString());
                    return searchResult.toBuilder().finalScore(finalScore).hybridScore(finalScore).build();
                })
                .sorted(Comparator.comparing(SearchResult::getFinalScore).reversed())
                .collect(Collectors.toList());
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
            log.info("Performing lexical search for query: '{}'", query);
            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index(SearchConstants.POSTS_INDEX)
                            .size(generalSearchProperties.getSearchSize())
                            .query(lexicalQuery),
                    PostDocument.class
            );
            log.info("Lexical search found {} hits.", response.hits().hits().size());
            return response.hits().hits();
        } catch (IOException e) {
            log.error("Elasticsearch lexical search failed [query={}]", query, e);
            throw new RuntimeException("Elasticsearch lexical search failed.", e);
        }
    }

    private List<Hit<PostDocument>> performSemanticSearch(List<Float> queryVector) {
        Query semanticQuery = Query.of(q -> q
                .scriptScore(ss -> ss
                        .query(Query.of(iq -> iq.matchAll(ma -> ma)))
                        .script(s -> s
                                .source(SearchConstants.SCRIPT_SOURCE_SEMANTIC)
                                .params(Map.of(SearchConstants.QUERY_VECTOR_PARAM, JsonData.of(queryVector)))
                        )
                )
        );

        try {
            log.info("Performing semantic search.");
            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index(SearchConstants.POSTS_INDEX)
                            .size(generalSearchProperties.getSearchSize())
                            .query(semanticQuery),
                    PostDocument.class
            );
            log.info("Semantic search found {} hits.", response.hits().hits().size());
            return response.hits().hits();
        } catch (IOException e) {
            log.error("Elasticsearch semantic search failed", e);
            throw new RuntimeException("Elasticsearch semantic search failed.", e);
        }
    }

    private SearchResult mapToSearchResult(Hit<PostDocument> hit) {
        PostDocument doc = hit.source();
        double score = Objects.requireNonNullElse(hit.score(), 0.0);

        float[] vector = VectorUtil.convertToFloatArray(Objects.requireNonNull(doc).getSummaryEmbedding());

        return SearchResult.builder()
                .postId(doc.getPostId())
                .title(doc.getTitle())
                .summary(doc.getSummary())
                .companyName(doc.getCompany())
                .url(doc.getUrl())
                .hybridScore(score)
                .finalScore(score)
                .documentVector(vector)
                .build();
    }

    private List<SearchResult> personalReranking(List<SearchResult> initialResults, float[] userProfileVector) {
        return initialResults.stream()
                .filter(result -> result.getDocumentVector() != null && result.getDocumentVector().length > 0)
                .map(result -> {
                    double personalScore = VectorUtil.cosineSimilarity(userProfileVector, result.getDocumentVector());
                    double finalScore = (result.getHybridScore() * generalSearchProperties.getHybridScoreWeight())
                            + (personalScore * generalSearchProperties.getPersonalScoreWeight());

                    return result.toBuilder()
                            .personalScore(personalScore)
                            .finalScore(finalScore)
                            .documentVector(null)
                            .build();
                })
                .sorted(Comparator.comparing(SearchResult::getFinalScore).reversed())
                .collect(Collectors.toList());
    }
}
