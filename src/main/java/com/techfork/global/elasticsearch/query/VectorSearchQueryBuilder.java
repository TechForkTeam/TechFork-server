package com.techfork.global.elasticsearch.query;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 벡터 검색 쿼리 빌더 구현체
 * script_score를 사용한 코사인 유사도 검색 쿼리 생성
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VectorSearchQueryBuilder implements VectorQueryBuilder {

    private static final String COSINE_SIMILARITY_SCRIPT_TEMPLATE = "cosineSimilarity(params.query_vector, '%s') + 1.0";
    private static final String QUERY_VECTOR_PARAM = "query_vector";

    @Override
    public Query createWeightedVectorQuery(
            String titleField,
            String summaryField,
            String contentChunksPath,
            String chunkEmbeddingField,
            float[] queryVector,
            float titleWeight,
            float summaryWeight,
            float contentWeight
    ) {
        Query titleQuery = createScriptScoreQuery(titleField, queryVector, titleWeight);
        Query summaryQuery = createScriptScoreQuery(summaryField, queryVector, summaryWeight);
        Query chunkQuery = createNestedScriptScoreQuery(
                contentChunksPath,
                chunkEmbeddingField,
                queryVector,
                contentWeight,
                ChildScoreMode.Max
        );

        return Query.of(q -> q
                .bool(b -> b
                        .should(titleQuery)
                        .should(summaryQuery)
                        .should(chunkQuery)
                )
        );
    }

    @Override
    public Query createWeightedVectorQueryWithRandomness(
            String titleField,
            String summaryField,
            String contentChunksPath,
            String chunkEmbeddingField,
            float[] queryVector,
            float titleWeight,
            float summaryWeight,
            float contentWeight,
            long randomSeed,
            double randomWeight
    ) {
        // 기본 벡터 쿼리 생성
        Query baseQuery = createWeightedVectorQuery(
                titleField,
                summaryField,
                contentChunksPath,
                chunkEmbeddingField,
                queryVector,
                titleWeight,
                summaryWeight,
                contentWeight
        );

        // function_score로 랜덤 요소 추가
        return Query.of(q -> q
                .functionScore(fs -> fs
                        .query(baseQuery)
                        .functions(fn -> fn
                                .randomScore(rs -> rs
                                        .seed(String.valueOf(randomSeed))
                                        .field("_seq_no")
                                )
                                .weight(randomWeight)
                        )
                        .boostMode(co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode.Sum)
                )
        );
    }

    @Override
    public Query createScriptScoreQuery(String fieldName, float[] queryVector, float boost) {
        String script = String.format(COSINE_SIMILARITY_SCRIPT_TEMPLATE, fieldName);

        return Query.of(q -> q
                .scriptScore(ss -> ss
                        .query(mq -> mq.matchAll(m -> m))
                        .script(s -> s
                                .source(script)
                                .params(QUERY_VECTOR_PARAM, JsonData.of(queryVector))
                        )
                        .boost(boost)
                )
        );
    }

    @Override
    public Query createNestedScriptScoreQuery(
            String nestedPath,
            String vectorFieldName,
            float[] queryVector,
            float boost,
            ChildScoreMode scoreMode
    ) {
        String fullPath = nestedPath + "." + vectorFieldName;
        String script = String.format(COSINE_SIMILARITY_SCRIPT_TEMPLATE, fullPath);

        return Query.of(q -> q
                .nested(n -> n
                        .path(nestedPath)
                        .scoreMode(ChildScoreMode.Max)
                        .query(nq -> nq
                                .scriptScore(ss -> ss
                                        .query(mq -> mq.matchAll(m -> m))
                                        .script(s -> s
                                                .source(script)
                                                .params(QUERY_VECTOR_PARAM, JsonData.of(queryVector))
                                        )
                                )
                        )
                        .boost(boost)
                )
        );
    }
}
