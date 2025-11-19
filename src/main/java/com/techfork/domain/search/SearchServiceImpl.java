package com.techfork.domain.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.global.llm.EmbeddingClient;
import java.io.IOException;
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

    // 하이퍼파라미터
    private final GeneralSearchProperties generalSearchProperties;

    @Override
    public List<SearchResult> searchGeneral(String query) {
        List<Float> queryVector = embeddingClient.embed(query);

        // title - 가중치 부여
        String titleField = String.format("title^%.1f", generalSearchProperties.getTitleBoost());

        // rrf - es 라이센스 필요로 잠시 보류
        /*
        try {
            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index("posts")
                            .size(generalSearchProperties.getSearchSize())
                            .query(q -> q
                                    .bool(b -> b
                                            .should(sh -> sh
                                                    .multiMatch(m -> m
                                                            .query(query)
                                                            .fields(titleField, "summary")
                                                    )
                                            )
                                    )
                            )
                            .knn(k -> k
                                    .field("summaryEmbedding")
                                    .queryVector(queryVector)
                                    .k(generalSearchProperties.getKnnK())
                                    .numCandidates(generalSearchProperties.getKnnNumCandidates())
                            )
                            .rank(r -> r
                                    .rrf(rrf -> rrf
                                            .rankWindowSize(generalSearchProperties.getRrfWindowSize())
                                            .rankConstant(generalSearchProperties.getRrfRankConstant())
                                    )
                            ),
                    PostDocument.class
            );

            // 3. 결과 매핑
            return response.hits().hits().stream()
                    .map(this::mapToSearchResult)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Elasticsearch searchGeneral failed [query={}]", query, e);
            throw new RuntimeException("Elasticsearch 검색 중 오류가 발생했습니다.", e);
        }
        */
        try {
            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index("posts")
                            .size(generalSearchProperties.getSearchSize())
                            .query(q -> q
                                    .bool(b -> b
                                            .should(sh -> sh
                                                    .multiMatch(m -> m
                                                            .query(query)
                                                            .fields(titleField, "summary")
                                                    )
                                            )
                                            .should(sh -> sh
                                                    .knn(k -> k
                                                            .field("summaryEmbedding")
                                                            .queryVector(queryVector)
                                                            .k(generalSearchProperties.getKnnK())
                                                            .numCandidates(generalSearchProperties.getKnnNumCandidates())
                                                    )
                                            )
                                    )
                            )
                    ,
                    PostDocument.class
            );

            // 3. 결과 매핑
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

    /**
     * Elasticsearch 검색 결과 Hit을 SearchResult DTO로 변환
     */
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
                .finalScore(score) // 1단계에서는 개인화 점수 없이 hybridScore가 finalScore
                .documentVector(null)
                .build();
    }
}