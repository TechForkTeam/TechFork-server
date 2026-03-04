package com.techfork.global.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.user.document.UserProfileDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ElasticsearchWarmupManager implements ApplicationRunner {

    private static final String POSTS_INDEX = "posts";
    private static final String USER_PROFILES_INDEX = "user_profiles";
    private static final int EMBEDDING_DIMENSION = 3072;

    private final ElasticsearchClient elasticsearchClient;
    private final RecommendationProperties recommendationProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[ES Warmup] Starting initial warmup...");
        warmupClusterHealth();
        warmupIndex();
        log.info("[ES Warmup] Initial warmup completed");
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void keepAliveWarmup() {
        log.debug("[ES Warmup] Keep-alive warmup starting...");
        warmupIndex();
        log.debug("[ES Warmup] Keep-alive warmup completed");
    }

    private void warmupClusterHealth() {
        try {
            elasticsearchClient.cluster().health();
            log.debug("[ES Warmup] Cluster health check OK");
        } catch (Exception e) {
            log.warn("[ES Warmup] Cluster health check failed: {}", e.getMessage());
        }
    }

    private void warmupIndex() {
        warmupLexicalSearch();
        warmupPostsKnnSearch();
        warmupUserProfilesKnnSearch();
    }

    private void warmupLexicalSearch() {
        try {
            elasticsearchClient.search(s -> s
                            .index(POSTS_INDEX)
                            .size(1)
                            .query(q -> q
                                    .multiMatch(m -> m
                                            .query("technology")
                                            .fields("title^3.0", "summary^1.5", "contentChunks.chunkText")
                                    )
                            ),
                    PostDocument.class
            );
            log.debug("[ES Warmup] posts lexical warmup OK");
        } catch (Exception e) {
            log.warn("[ES Warmup] posts lexical warmup failed: {}", e.getMessage());
        }
    }

    private void warmupPostsKnnSearch() {
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

    private void warmupUserProfilesKnnSearch() {
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
