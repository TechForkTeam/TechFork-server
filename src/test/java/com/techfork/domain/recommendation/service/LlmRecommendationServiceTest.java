package com.techfork.domain.recommendation.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import com.techfork.activity.readpost.application.query.lookup.ReadPostLookupService;
import com.techfork.global.elasticsearch.query.VectorQueryBuilder;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.fixture.PostDocumentFixture;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.recommendation.repository.RecommendationHistoryRepository;
import com.techfork.post.domain.Post;
import com.techfork.global.util.TimeDecayStrategy;
import com.techfork.personalization.application.query.lookup.PersonalizationProfileLookupItem;
import com.techfork.personalization.application.query.lookup.PersonalizationProfileLookupService;
import com.techfork.post.application.query.lookup.PostLookupService;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.fixture.UserFixture;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private PersonalizationProfileLookupService personalizationProfileLookupService;

    @Mock
    private RecommendedPostRepository recommendedPostRepository;

    @Mock
    private RecommendationHistoryRepository recommendationHistoryRepository;

    @Mock
    private ReadPostLookupService readPostLookupService;

    @Mock
    private PostLookupService postLookupService;

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
                personalizationProfileLookupService,
                recommendedPostRepository,
                recommendationHistoryRepository,
                readPostLookupService,
                postLookupService,
                mmrService,
                timeDecayStrategy,
                properties,
                vectorQueryBuilder,
                Runnable::run
        );
    }

    @Nested
    @DisplayName("generateRecommendationsForUser")
    class GenerateRecommendationsForUser {

        @Test
        @DisplayName("추천 생성은 personalization lookup의 벡터와 핵심 키워드로 후보를 검색한다")
        void storedProfileExists_UsesProfileVectorAndKeywords() throws IOException {
            Long userId = 9L;
            User user = createUser(userId);
            float[] profileVector = new float[]{0.1f, 0.2f};
            PersonalizationProfileLookupItem personalizationProfile = new PersonalizationProfileLookupItem(
                    profileVector,
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

            given(personalizationProfileLookupService.findByUserId(userId))
                    .willReturn(Optional.of(personalizationProfile));
            Set<Long> readPostIds = Set.of(301L);
            given(readPostLookupService.getRecentReadPostIds(userId, 1000)).willReturn(readPostIds);
            given(vectorQueryBuilder.createExcludeFilter(readPostIds)).willReturn(filterQuery);
            given(vectorQueryBuilder.createKnnSearches(
                    eq("titleEmbedding"),
                    eq("summaryEmbedding"),
                    eq("contentChunks.embedding"),
                    aryEq(profileVector),
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
            given(postLookupService.getPostReference(501L)).willReturn(recommendedPost);
            given(recommendedPostRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

            int createdCount = llmRecommendationService.generateRecommendationsForUser(user);

            assertThat(createdCount).isEqualTo(1);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<MmrService.MmrCandidate>> candidatesCaptor = ArgumentCaptor.forClass(List.class);
            verify(mmrService).applyMmr(candidatesCaptor.capture());
            assertThat(candidatesCaptor.getValue())
                    .extracting(MmrService.MmrCandidate::getPostId)
                    .containsExactly(501L, 502L);
            verify(personalizationProfileLookupService, times(1)).findByUserId(userId);
            verify(readPostLookupService).getRecentReadPostIds(userId, 1000);
            verify(vectorQueryBuilder).createExcludeFilter(readPostIds);
            verify(postLookupService).getPostReference(501L);
            verify(vectorQueryBuilder).createKnnSearches(
                    eq("titleEmbedding"),
                    eq("summaryEmbedding"),
                    eq("contentChunks.embedding"),
                    aryEq(profileVector),
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
        @DisplayName("저장된 개인화 프로필이 없으면 추천 생성을 건너뛴다")
        void storedProfileMissing_SkipsRecommendationGeneration() {
            Long userId = 11L;
            User user = createUser(userId);
            given(personalizationProfileLookupService.findByUserId(userId)).willReturn(Optional.empty());

            int createdCount = llmRecommendationService.generateRecommendationsForUser(user);

            assertThat(createdCount).isZero();
            verify(personalizationProfileLookupService).findByUserId(userId);
            verify(readPostLookupService, never()).getRecentReadPostIds(any(), anyInt());
        }

        @Test
        @DisplayName("프로필 스냅샷 기반 추천 생성은 PersonalizationProfileDocument를 다시 조회하지 않는다")
        void profileSnapshotProvided_DoesNotReadStoredProfile() throws IOException {
            Long userId = 10L;
            User user = createUser(userId);
            float[] profileVector = new float[]{0.1f, 0.2f};
            List<String> keyKeywords = List.of("Spring", "JPA");
            Query filterQuery = Query.of(query -> query.matchAll(matchAll -> matchAll));
            Query bm25Query = Query.of(query -> query.matchAll(matchAll -> matchAll));

            given(readPostLookupService.getRecentReadPostIds(userId, 1000)).willReturn(Set.of());
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
            verify(personalizationProfileLookupService, never()).findByUserId(any());
            verify(vectorQueryBuilder).createBm25Query(keyKeywords, 0.6f, 0.2f, 0.2f);
        }
    }

    @Nested
    @DisplayName("applyRrf")
    class ApplyRrf {

        @Test
        @DisplayName("추천 후보는 Post aggregate가 아니라 PostDocument projection에서 만들어진다")
        void candidateProjectionsProvided_UsesProjectionAsCandidateSource() {
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
        void candidateWithoutSummaryVector_FiltersOutCandidate() {
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
    }

    @Nested
    @DisplayName("mapToMmrCandidate")
    class MapToMmrCandidate {

        @Test
        @DisplayName("projection 임베딩과 발행 시각으로 MMR 후보를 만든다")
        void projectionProvided_UsesEmbeddingsAndPublishedAt() {
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
        return PostDocumentFixture.createPostDocument(
                postId,
                "추천 문서 " + postId,
                titleEmbedding,
                summaryEmbedding,
                publishedAt
        );
    }

    private User createUser(Long userId) {
        return UserFixture.socialUserWithId(
                userId,
                SocialType.KAKAO,
                "social-" + userId,
                "user" + userId + "@example.com",
                null
        );
    }
}
