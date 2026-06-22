package com.techfork.domain.recommendation.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocument;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocumentRepository;
import com.techfork.global.elasticsearch.query.VectorQueryBuilder;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.recommendation.repository.RecommendationHistoryRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.global.util.TimeDecayStrategy;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.fixture.UserFixture;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LlmRecommendationService 단위 테스트")
class LlmRecommendationServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @Mock
    private RecommendedPostRepository recommendedPostRepository;

    @Mock
    private RecommendationHistoryRepository recommendationHistoryRepository;

    @Mock
    private ReadPostRepository readPostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MmrService mmrService;

    @Mock
    private TimeDecayStrategy timeDecayStrategy;

    @Mock
    private VectorQueryBuilder vectorQueryBuilder;

    private LlmRecommendationService llmRecommendationService;

    @BeforeEach
    void setUp() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setMmrCandidateSize(5);
        llmRecommendationService = new LlmRecommendationService(
                elasticsearchClient,
                personalizationProfileDocumentRepository,
                recommendedPostRepository,
                recommendationHistoryRepository,
                readPostRepository,
                postRepository,
                mmrService,
                timeDecayStrategy,
                properties,
                vectorQueryBuilder,
                Runnable::run
        );
    }

    @Test
    @DisplayName("추천 생성은 PersonalizationProfileDocument projection의 벡터와 핵심 키워드로 후보를 검색한다")
    void generateRecommendationsForUser_UsesPersonalizationProfileProjectionVectorAndKeywords() throws IOException {
        Long userId = 9L;
        User user = createUser(userId);
        float[] profileVector = new float[]{0.1f, 0.2f};
        PersonalizationProfileDocument personalizationProfile = PersonalizationProfileDocument.create(
                userId,
                "Spring과 JPA 기반 백엔드 성능 개선에 관심이 높은 사용자",
                profileVector,
                List.of("Backend"),
                List.of("Spring", "JPA")
        );
        Query filterQuery = Query.of(query -> query.matchAll(matchAll -> matchAll));
        Query bm25Query = Query.of(query -> query.matchAll(matchAll -> matchAll));
        PostDocument vectorDocument = postDocument(
                501L,
                List.of(0.1f, 0.2f),
                List.of(0.3f, 0.4f),
                LocalDateTime.of(2026, 5, 4, 9, 0)
        );
        PostDocument keywordDocument = postDocument(
                502L,
                List.of(0.5f, 0.6f),
                List.of(0.7f, 0.8f),
                LocalDateTime.of(2026, 5, 5, 9, 0)
        );
        Post recommendedPost = mock(Post.class);

        given(personalizationProfileDocumentRepository.findByUserId(userId))
                .willReturn(Optional.of(personalizationProfile));
        given(readPostRepository.findRecentReadPostsByUserIdWithMinDuration(userId, PageRequest.of(0, 1000)))
                .willReturn(List.of());
        given(vectorQueryBuilder.createExcludeFilter(Set.of())).willReturn(filterQuery);
        given(vectorQueryBuilder.createKnnSearches(
                eq("titleEmbedding"),
                eq("summaryEmbedding"),
                eq("contentChunks.embedding"),
                same(profileVector),
                eq(0.6f),
                eq(0.2f),
                eq(0.2f),
                eq(50),
                eq(150),
                same(filterQuery)
        )).willReturn(List.of());
        given(vectorQueryBuilder.createBm25Query(List.of("Spring", "JPA"), 0.6f, 0.2f, 0.2f))
                .willReturn(bm25Query);
        given(elasticsearchClient.search(
                ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
                eq(PostDocument.class)
        ))
                .willReturn(
                        searchResponse(hit("501", 9.0, vectorDocument)),
                        searchResponse(hit("502", 7.0, keywordDocument))
                );
        given(timeDecayStrategy.calculateWeight(any())).willReturn(1.0);
        given(mmrService.applyMmr(anyList()))
                .willReturn(List.of(MmrService.MmrResult.builder()
                        .postId(501L)
                        .similarityScore(0.9)
                        .mmrScore(0.8)
                        .rank(1)
                        .build()));
        given(recommendedPostRepository.findByUserOrderByRankAsc(user)).willReturn(List.of());
        given(postRepository.getReferenceById(501L)).willReturn(recommendedPost);
        given(recommendedPostRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        int createdCount = llmRecommendationService.generateRecommendationsForUser(user);

        assertThat(createdCount).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MmrService.MmrCandidate>> candidatesCaptor = ArgumentCaptor.forClass(List.class);
        verify(mmrService).applyMmr(candidatesCaptor.capture());
        assertThat(candidatesCaptor.getValue())
                .extracting(MmrService.MmrCandidate::getPostId)
                .containsExactly(501L, 502L);
        verify(personalizationProfileDocumentRepository, times(1)).findByUserId(userId);
        verify(vectorQueryBuilder).createKnnSearches(
                eq("titleEmbedding"),
                eq("summaryEmbedding"),
                eq("contentChunks.embedding"),
                same(profileVector),
                eq(0.6f),
                eq(0.2f),
                eq(0.2f),
                eq(50),
                eq(150),
                same(filterQuery)
        );
        verify(vectorQueryBuilder).createBm25Query(List.of("Spring", "JPA"), 0.6f, 0.2f, 0.2f);
    }

    @Test
    @DisplayName("프로필 스냅샷 기반 추천 생성은 PersonalizationProfileDocument를 다시 조회하지 않는다")
    void generateRecommendationsForUser_WithProfileSnapshot_DoesNotReadPersonalizationProfileProjection() throws IOException {
        Long userId = 10L;
        User user = createUser(userId);
        float[] profileVector = new float[]{0.1f, 0.2f};
        List<String> keyKeywords = List.of("Spring", "JPA");
        Query filterQuery = Query.of(query -> query.matchAll(matchAll -> matchAll));
        Query bm25Query = Query.of(query -> query.matchAll(matchAll -> matchAll));

        given(readPostRepository.findRecentReadPostsByUserIdWithMinDuration(userId, PageRequest.of(0, 1000)))
                .willReturn(List.of());
        given(vectorQueryBuilder.createExcludeFilter(Set.of())).willReturn(filterQuery);
        given(vectorQueryBuilder.createKnnSearches(
                eq("titleEmbedding"),
                eq("summaryEmbedding"),
                eq("contentChunks.embedding"),
                same(profileVector),
                eq(0.6f),
                eq(0.2f),
                eq(0.2f),
                eq(50),
                eq(150),
                same(filterQuery)
        )).willReturn(List.of());
        given(vectorQueryBuilder.createBm25Query(keyKeywords, 0.6f, 0.2f, 0.2f))
                .willReturn(bm25Query);
        given(elasticsearchClient.search(
                ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
                eq(PostDocument.class)
        ))
                .willReturn(emptySearchResponse(), emptySearchResponse());

        int createdCount = llmRecommendationService.generateRecommendationsForUser(user, profileVector, keyKeywords);

        assertThat(createdCount).isZero();
        verify(personalizationProfileDocumentRepository, never()).findByUserId(any());
        verify(vectorQueryBuilder).createBm25Query(keyKeywords, 0.6f, 0.2f, 0.2f);
    }

    @Test
    @DisplayName("추천 후보는 Post aggregate가 아니라 PostDocument projection에서 만들어진다")
    void applyRrf_UsesPostDocumentProjectionAsCandidateSource() {
        PostDocument vectorDoc = postDocument(
                101L,
                List.of(0.1f, 0.2f),
                List.of(0.3f, 0.4f),
                LocalDateTime.of(2026, 5, 1, 9, 0)
        );
        PostDocument keywordDoc = postDocument(
                202L,
                List.of(0.5f, 0.6f),
                List.of(0.7f, 0.8f),
                LocalDateTime.of(2026, 5, 2, 9, 0)
        );
        given(timeDecayStrategy.calculateWeight(vectorDoc.getPublishedAt())).willReturn(1.2);
        given(timeDecayStrategy.calculateWeight(keywordDoc.getPublishedAt())).willReturn(0.8);

        List<MmrService.MmrCandidate> candidates = llmRecommendationService.applyRrf(
                List.of(hit("101", 9.0, vectorDoc)),
                List.of(hit("202", 7.0, keywordDoc))
        );

        assertThat(candidates).hasSize(2);
        assertThat(candidates)
                .extracting(MmrService.MmrCandidate::getPostId)
                .containsExactly(101L, 202L);
        assertThat(candidates.get(0).getTitleVector()).containsExactly(0.1f, 0.2f);
        assertThat(candidates.get(0).getSummaryVector()).containsExactly(0.3f, 0.4f);
        assertThat(candidates.get(1).getTitleVector()).containsExactly(0.5f, 0.6f);
        assertThat(candidates.get(1).getSummaryVector()).containsExactly(0.7f, 0.8f);
        assertThat(candidates).allSatisfy(candidate -> assertThat(candidate.getSimilarityScore()).isPositive());
        verify(timeDecayStrategy, times(1)).calculateWeight(vectorDoc.getPublishedAt());
        verify(timeDecayStrategy, times(1)).calculateWeight(keywordDoc.getPublishedAt());
    }

    @Test
    @DisplayName("summary embedding이 없는 후보는 applyRrf 결과에서 제외한다")
    void applyRrf_FiltersOutCandidatesWithoutSummaryVector() {
        PostDocument missingSummaryDoc = postDocument(
                404L,
                List.of(0.2f, 0.3f),
                null,
                LocalDateTime.of(2026, 5, 3, 9, 0)
        );
        given(timeDecayStrategy.calculateWeight(missingSummaryDoc.getPublishedAt())).willReturn(1.0);

        List<MmrService.MmrCandidate> candidates = llmRecommendationService.applyRrf(
                List.of(hit("404", 3.0, missingSummaryDoc)),
                List.of()
        );

        assertThat(candidates).isEmpty();
        verify(timeDecayStrategy).calculateWeight(missingSummaryDoc.getPublishedAt());
    }

    @Test
    @DisplayName("projection 임베딩과 발행 시각으로 MMR 후보를 만든다")
    void mapToMmrCandidate_UsesProjectionEmbeddingsAndPublishedAt() {
        PostDocument document = postDocument(
                303L,
                List.of(0.9f, 0.8f),
                List.of(0.7f, 0.6f),
                LocalDateTime.of(2026, 5, 1, 9, 0)
        );
        given(timeDecayStrategy.calculateWeight(document.getPublishedAt())).willReturn(1.5);

        MmrService.MmrCandidate candidate = llmRecommendationService.mapToMmrCandidate(
                hit("303", 5.0, document),
                0.4
        );

        assertThat(candidate.getPostId()).isEqualTo(303L);
        assertThat(candidate.getTitleVector()).containsExactly(0.9f, 0.8f);
        assertThat(candidate.getSummaryVector()).containsExactly(0.7f, 0.6f);
        assertThat(candidate.getSimilarityScore()).isCloseTo(0.6, within(1e-9));
        verify(timeDecayStrategy).calculateWeight(document.getPublishedAt());
    }

    private SearchResponse<PostDocument> searchResponse(Hit<PostDocument> hit) {
        return SearchResponse.of(response -> response
                .took(1)
                .timedOut(false)
                .shards(shards -> shards
                        .total(1)
                        .successful(1)
                        .failed(0)
                )
                .hits(hits -> hits.hits(hit))
        );
    }

    private SearchResponse<PostDocument> emptySearchResponse() {
        return SearchResponse.of(response -> response
                .took(1)
                .timedOut(false)
                .shards(shards -> shards
                        .total(1)
                        .successful(1)
                        .failed(0)
                )
                .hits(hits -> hits.hits(List.of()))
        );
    }

    private Hit<PostDocument> hit(String id, double score, PostDocument document) {
        return Hit.of(hit -> hit
                .id(id)
                .index("posts")
                .score(score)
                .source(document)
        );
    }

    private PostDocument postDocument(
            Long postId,
            List<Float> titleEmbedding,
            List<Float> summaryEmbedding,
            LocalDateTime publishedAt
    ) {
        return PostDocument.builder()
                .id(String.valueOf(postId))
                .postId(postId)
                .title("추천 문서 " + postId)
                .summary("추천 요약 " + postId)
                .shortSummary("짧은 추천 요약")
                .company("TechFork")
                .url("https://posts.example.com/" + postId)
                .logoUrl("https://cdn.example.com/logo.png")
                .thumbnailUrl("https://cdn.example.com/thumb.png")
                .publishedAtString(publishedAt.toString())
                .titleEmbedding(titleEmbedding)
                .summaryEmbedding(summaryEmbedding)
                .contentChunks(List.of())
                .build();
    }

    private User createUser(Long userId) {
        User user = UserFixture.socialUser("social-" + userId, "user" + userId + "@example.com", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
