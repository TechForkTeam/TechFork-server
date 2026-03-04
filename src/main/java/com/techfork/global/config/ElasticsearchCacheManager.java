package com.techfork.global.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.json.JsonData;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.user.document.UserProfileDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchCacheManager implements ApplicationRunner {

    private static final String POSTS_INDEX = "posts";
    private static final String USER_PROFILES_INDEX = "user_profiles";
    private static final List<String> INDICES = List.of(POSTS_INDEX, USER_PROFILES_INDEX);
    private static final int EMBEDDING_DIMENSION = 3072;

    private final ElasticsearchClient elasticsearchClient;
    private final RecommendationProperties recommendationProperties;

    @Override
    public void run(ApplicationArguments args) {
        INDICES.forEach(this::applyPreload);
        keepAliveWarmup();
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void keepAliveWarmup() {
        warmupPostsKnn();
        warmupUserProfilesKnn();
    }

    private void applyPreload(String index) {
        try {
            // store.preload는 static setting이라 reopen=true 필요 (ES가 자동으로 닫았다 열어줌)
            // vec: HNSW 그래프, vem: HNSW 메타데이터
            elasticsearchClient.indices().putSettings(s -> s
                    .index(index)
                    .reopen(true)
                    .settings(is -> is
                            .otherSettings("store.preload", JsonData.of(List.of("vec", "vem")))
                    )
            );
            log.info("[ES Index] preload applied to '{}'", index);
        } catch (Exception e) {
            log.warn("[ES Index] preload failed for '{}': {}", index, e.getMessage());
        }
    }

    private void warmupPostsKnn() {
        List<Float> dummyVector = createDummyVector();
        List<KnnSearch> knnSearches = List.of(
                createKnn("titleEmbedding", dummyVector),
                createKnn("summaryEmbedding", dummyVector),
                createKnn("contentChunks.embedding", dummyVector)
        );
        try {
            elasticsearchClient.search(s -> s
                            .index(POSTS_INDEX)
                            .size(1)
                            .knn(knnSearches),
                    PostDocument.class
            );
            log.debug("[ES Warmup] posts kNN warmup OK");
        } catch (Exception e) {
            log.warn("[ES Warmup] posts kNN warmup failed: {}", e.getMessage());
        }
    }

    private void warmupUserProfilesKnn() {
        try {
            List<Float> dummyVector = createDummyVector();
            elasticsearchClient.search(s -> s
                            .index(USER_PROFILES_INDEX)
                            .size(1)
                            .knn(List.of(createKnn("profileVector", dummyVector))),
                    UserProfileDocument.class
            );
            log.debug("[ES Warmup] user_profiles kNN warmup OK");
        } catch (Exception e) {
            log.warn("[ES Warmup] user_profiles kNN warmup failed: {}", e.getMessage());
        }
    }

    private KnnSearch createKnn(String field, List<Float> vector) {
        return KnnSearch.of(ks -> ks
                .field(field)
                .queryVector(vector)
                .k(recommendationProperties.getKnnSearchSize())
                .numCandidates(recommendationProperties.getNumCandidates())
        );
    }

    private List<Float> createDummyVector() {
        List<Float> vector = new ArrayList<>(EMBEDDING_DIMENSION);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            vector.add(random.nextFloat() * 2 - 1);
        }
        return vector;
    }
}