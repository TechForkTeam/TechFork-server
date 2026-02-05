package com.techfork.global.elasticsearch.query;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch 벡터 검색 쿼리 빌더 구현체
 * 네이티브 k-NN 검색 및 하이브리드 검색을 위한 쿼리 생성 제공
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VectorSearchQueryBuilder implements VectorQueryBuilder {

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
    public Query createRandomScoreQuery(long randomSeed, double randomWeight) {
        return Query.of(q -> q
                .functionScore(fs -> fs
                        .query(mq -> mq.matchAll(m -> m))
                        .functions(fn -> fn
                                .randomScore(rs -> rs
                                        .seed(String.valueOf(randomSeed))
                                        .field("_seq_no")
                                )
                                .weight(randomWeight)
                        )
                        .boostMode(FunctionBoostMode.Sum)
                )
        );
    }
}