package com.techfork.global.elasticsearch.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Elasticsearch 벡터 검색 쿼리 빌더 구현체
 * 네이티브 k-NN 검색 및 하이브리드 검색을 위한 쿼리 생성 제공
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VectorSearchQueryBuilder implements VectorQueryBuilder {

    @Override
    public Query createExcludeFilter(Set<Long> readPostIds) {
        if (readPostIds == null || readPostIds.isEmpty()) {
            return null;
        }

        List<FieldValue> excludeValues = readPostIds.stream()
                .map(FieldValue::of)
                .toList();

        return Query.of(q -> q
                .bool(b -> b
                        .mustNot(mn -> mn
                                .terms(t -> t
                                        .field("postId")
                                        .terms(v -> v.value(excludeValues))
                                )
                        )
                )
        );
    }

    @Override
    public List<KnnSearch> createKnnSearches(
            String titleField,
            String summaryField,
            String contentField,
            float[] queryVector,
            float titleWeight,
            float summaryWeight,
            float contentWeight,
            int k,
            int numCandidates,
            Query filter
    ) {
        List<KnnSearch> knnSearches = new ArrayList<>();
        List<Float> vectorList = new ArrayList<>();
        for (float v : queryVector) {
            vectorList.add(v);
        }

        if (titleWeight > 0) {
            knnSearches.add(KnnSearch.of(ks -> {
                ks.field(titleField)
                        .queryVector(vectorList)
                        .k(k)
                        .numCandidates(numCandidates)
                        .boost(titleWeight);
                if (filter != null) {
                    ks.filter(filter);
                }
                return ks;
            }));
        }

        if (summaryWeight > 0) {
            knnSearches.add(KnnSearch.of(ks -> {
                ks.field(summaryField)
                        .queryVector(vectorList)
                        .k(k)
                        .numCandidates(numCandidates)
                        .boost(summaryWeight);
                if (filter != null) {
                    ks.filter(filter);
                }
                return ks;
            }));
        }

        if (contentWeight > 0 && contentField != null) {
            knnSearches.add(KnnSearch.of(ks -> {
                ks.field(contentField)
                        .queryVector(vectorList)
                        .k(k)
                        .numCandidates(numCandidates)
                        .boost(contentWeight);
                if (filter != null) {
                    ks.filter(filter);
                }
                return ks;
            }));
        }

        return knnSearches;
    }

    @Override
    public Query createBm25Query(List<String> keywords, float titleBoost, float summaryBoost, float contentBoost) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }

        String combinedKeywords = String.join(" ", keywords);

        return Query.of(q -> q
                .bool(b -> b
                        .should(s -> s
                                .match(m -> m
                                        .field("title")
                                        .query(combinedKeywords)
                                        .boost(titleBoost)
                                )
                        )
                        .should(s -> s
                                .match(m -> m
                                        .field("summary")
                                        .query(combinedKeywords)
                                        .boost(summaryBoost)
                                )
                        )
                        .should(s -> s
                                .nested(n -> n
                                        .path("contentChunks")
                                        .query(nq -> nq
                                                .match(m -> m
                                                        .field("contentChunks.text")
                                                        .query(combinedKeywords)
                                                )
                                        )
                                        .boost(contentBoost)
                                )
                        )
                        .minimumShouldMatch("1")
                )
        );
    }
}