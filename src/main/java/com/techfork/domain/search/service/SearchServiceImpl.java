package com.techfork.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.user.entity.User;
import com.techfork.global.llm.EmbeddingClient;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingClient embeddingClient;
    private final GeneralSearchProperties generalSearchProperties;

    @Override
    public List<SearchResult> searchGeneral(String query) {
        List<Float> queryVector = embeddingClient.embed(query);
        String titleField = String.format("title^%.1f", generalSearchProperties.getTitleBoost());
        String summaryField = String.format("summary^%.1f", generalSearchProperties.getSummaryBoost());

        try {
            Query baseQuery = Query.of(q -> q
                    .bool(b -> b
                            .should(sh -> sh
                                    .multiMatch(m -> m
                                            .query(query)
                                            .fields(titleField, summaryField)
                                    )
                            )
                            .should(sh -> sh
                                    .nested(n -> n
                                            .path("contentChunks")
                                            .query(nq -> nq
                                                    .match(m -> m
                                                            .field("contentChunks.chunkText")
                                                            .query(query)
                                                    )
                                            )
                                            .boost(generalSearchProperties.getChunkBoost())
                                    )
                            )
                            .minimumShouldMatch("0")
                    )
            );

            FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.of(fs -> fs
                    .query(baseQuery)
                    .functions(f -> f
                            .scriptScore(ss -> ss
                                    .script(s -> s
                                            .source("cosineSimilarity(params.query_vector, 'summaryEmbedding') + 1.0")
                                            .params(Map.of(
                                                    "query_vector", JsonData.of(queryVector)
                                            ))
                                    )
                            )
                            .weight((double) generalSearchProperties.getSemanticBoost())
                    )
                    .scoreMode(FunctionScoreMode.Sum)
                    .boostMode(FunctionBoostMode.Sum)
            );

            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index("posts")
                            .size(generalSearchProperties.getSearchSize())
                            .query(q -> q.functionScore(functionScoreQuery))
                    ,
                    PostDocument.class
            );

            return response.hits().hits().stream()
                    .map(this::mapToSearchResult)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Elasticsearch searchGeneral failed [query={}]", query, e);
            throw new RuntimeException("Elasticsearch 검색 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<SearchResult> searchPersonalized(String query, User user) {
        return List.of();
    }

    private SearchResult mapToSearchResult(Hit<PostDocument> hit) {
        PostDocument doc = hit.source();
        double score = hit.score() != null ? hit.score() : 0.0;

        float[] vector = null;
        if (doc != null && doc.getSummaryEmbedding() != null) {
            List<Float> embedding = doc.getSummaryEmbedding();
            vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i);
            }
        }

        return SearchResult.builder()
                .postId(doc != null ? doc.getPostId() : null)
                .title(doc != null ? doc.getTitle() : "")
                .summary(doc != null ? doc.getSummary() : "")
                .companyName(doc != null ? doc.getCompany() : "")
                .url(doc != null ? doc.getUrl() : "")
                .hybridScore(score)
                .finalScore(score)
                .documentVector(null)
                .build();
    }
}