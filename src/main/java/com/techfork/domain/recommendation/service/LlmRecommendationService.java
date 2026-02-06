package com.techfork.domain.recommendation.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.global.elasticsearch.query.VectorQueryBuilder;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.entity.RecommendationHistory;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.recommendation.repository.RecommendationHistoryRepository;
import com.techfork.domain.recommendation.service.MmrService.MmrCandidate;
import com.techfork.domain.recommendation.service.MmrService.MmrResult;
import com.techfork.domain.user.document.UserProfileDocument;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.global.util.TimeDecayStrategy;
import com.techfork.global.util.VectorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MMR 알고리즘 기반 추천 전략 구현
 * - Elasticsearch k-NN 검색으로 초기 후보군 수집
 * - MMR 알고리즘으로 다양성 보장
 * - 읽은 글 제외 필터링 (Pre-filtering)
 * - 시간 감쇠 가중치 적용 (최신 게시글 우선)
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LlmRecommendationService implements RecommendationService {

    private final ElasticsearchClient elasticsearchClient;
    private final UserProfileDocumentRepository userProfileDocumentRepository;
    private final RecommendedPostRepository recommendedPostRepository;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    private final ReadPostRepository readPostRepository;
    private final PostRepository postRepository;
    private final MmrService mmrService;
    private final TimeDecayStrategy timeDecayStrategy;
    private final RecommendationProperties properties;
    private final VectorQueryBuilder vectorQueryBuilder;

    private static final String POSTS_INDEX = "posts";
    private static final String TITLE_EMBEDDING_FIELD = "titleEmbedding";
    private static final String SUMMARY_EMBEDDING_FIELD = "summaryEmbedding";
    private static final String CONTENT_CHUNKS_EMBEDDING_FIELD = "contentChunks.embedding";

    @Override
    public int generateRecommendationsForUser(User user) {
        log.info("사용자 {} 추천 생성 시작", user.getId());

        // 1. 사용자 프로필 벡터 조회
        Optional<UserProfileDocument> profileOpt = userProfileDocumentRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty() || profileOpt.get().getProfileVector() == null) {
            log.warn("사용자 {}의 프로필 또는 벡터를 찾을 수 없음. 추천 생성 스킵.", user.getId());
            return 0;
        }

        UserProfileDocument profile = profileOpt.get();
        float[] userProfileVector = profile.getProfileVector();

        try {
            // 2. k-NN 검색으로 초기 후보군 가져오기
            List<MmrCandidate> candidates = searchCandidates(userProfileVector, user);

            if (candidates.isEmpty()) {
                log.info("사용자 {}의 추천 후보군을 찾을 수 없음", user.getId());
                return 0;
            }

            log.info("사용자 {} 추천 후보 {} 개 발견", user.getId(), candidates.size());

            // 3. MMR 적용하여 최종 추천 선택
            List<MmrResult> mmrResults = mmrService.applyMmr(candidates);

            // 4. 기존 추천을 이력으로 보관 (오늘 생성된 추천 포함)
            List<RecommendedPost> oldRecommendations = recommendedPostRepository.findByUserOrderByRankAsc(user);

            if (!oldRecommendations.isEmpty()) {
                List<RecommendationHistory> histories = oldRecommendations.stream()
                        .map(RecommendationHistory::fromRecommendedPost)
                        .toList();
                recommendationHistoryRepository.saveAll(histories);
                recommendedPostRepository.deleteByUser(user);
            }

            // 5. 새 추천 저장
            List<RecommendedPost> recommendations = new ArrayList<>();
            for (MmrResult result : mmrResults) {
                Post post = postRepository.getReferenceById(result.getPostId());
                RecommendedPost recommendedPost = RecommendedPost.create(
                        user,
                        post,
                        result.getSimilarityScore(),
                        result.getMmrScore(),
                        result.getRank()
                );
                recommendations.add(recommendedPost);
            }

            recommendedPostRepository.saveAll(recommendations);

            log.info("사용자 {} 추천 생성 완료: {} 개", user.getId(), recommendations.size());

            return recommendations.size();

        } catch (Exception e) {
            log.error("사용자 {} 추천 생성 실패", user.getId(), e);
            throw new RuntimeException("추천 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 추천 생성 (평가 전용 - Train/Test Split 지원)
     * 특정 읽은 글 목록(Train Set)만 제외하고 추천 생성
     *
     * @param user 사용자
     * @param trainPostIds Train Set 게시글 ID 목록 (제외할 글)
     * @return 추천된 게시글 ID 리스트
     */
    public List<Long> generateRecommendationsForEvaluation(User user, Set<Long> trainPostIds) {
        // 1. 사용자 프로필 벡터 조회
        Optional<UserProfileDocument> profileOpt = userProfileDocumentRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty() || profileOpt.get().getProfileVector() == null) {
            log.warn("사용자 {}의 프로필 또는 벡터를 찾을 수 없음. 추천 생성 스킵.", user.getId());
            return Collections.emptyList();
        }

        float[] userProfileVector = profileOpt.get().getProfileVector();

        try {
            // 2. k-NN 검색으로 초기 후보군 가져오기 (Train Set만 제외)
            List<MmrCandidate> candidates = searchCandidatesWithCustomReadHistory(userProfileVector, user, trainPostIds);

            if (candidates.isEmpty()) {
                log.debug("사용자 {}의 추천 후보군을 찾을 수 없음 (Train Set {} 개 제외)", user.getId(), trainPostIds.size());
                return Collections.emptyList();
            }

            // 3. MMR 적용하여 최종 추천 선택
            List<MmrResult> mmrResults = mmrService.applyMmr(candidates);

            // 4. 추천된 게시글 ID 리스트 반환
            return mmrResults.stream()
                    .map(MmrResult::getPostId)
                    .toList();

        } catch (Exception e) {
            log.error("사용자 {} 추천 생성 실패 (Train/Test Split 평가용)", user.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Elasticsearch 네이티브 k-NN 검색으로 초기 후보군 조회 (커스텀 읽은 글 목록)
     * Train/Test Split 평가를 위해 Train Set만 제외
     */
    private List<MmrCandidate> searchCandidatesWithCustomReadHistory(
            float[] userProfileVector,
            User user,
            Set<Long> readPostIds) throws IOException {

        log.debug("사용자 {}의 읽은 게시글 {} 개 제외 (Train Set)", user.getId(), readPostIds.size());

        // 가중치 가져오기
        RecommendationProperties.EmbeddingWeights weights = properties.getEmbeddingWeights();

        // 1. 읽은 글 제외 필터 쿼리 생성 (Pre-filtering)
        Query filterQuery = createExcludeFilter(readPostIds);

        // 2. 네이티브 k-NN 검색 객체 리스트 생성 (Title + Summary + Content)
        List<KnnSearch> knnSearches = vectorQueryBuilder.createKnnSearches(
                TITLE_EMBEDDING_FIELD,
                SUMMARY_EMBEDDING_FIELD,
                CONTENT_CHUNKS_EMBEDDING_FIELD,
                userProfileVector,
                weights.getTitle(),
                weights.getSummary(),
                weights.getContent(),
                properties.getKnnSearchSize(),
                properties.getNumCandidates(),
                filterQuery
        );

        log.debug("ES k-NN 검색 실행 (Train/Test Split) - 가중치 [title:{}, summary:{}, content:{}]",
                weights.getTitle(), weights.getSummary(), weights.getContent());

        long startTime = System.currentTimeMillis();
        SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                        .index(POSTS_INDEX)
                        .knn(knnSearches)       // k-NN 검색 (관련성 + 필터링)
                        .size(properties.getKnnSearchSize())
                ,
                PostDocument.class
        );
        long duration = System.currentTimeMillis() - startTime;
        log.info("추천 후보군 검색 완료 (Evaluation): {} 개, 소요 시간: {}ms", response.hits().hits().size(), duration);

        // 결과를 MmrCandidate로 변환
        return response.hits().hits().stream()
                .filter(hit -> hit.source() != null)
                .map(this::mapToMmrCandidate)
                .filter(candidate -> candidate.getSummaryVector() != null)
                .toList();
    }

    /**
     * Elasticsearch 네이티브 k-NN 검색으로 초기 후보군 조회
     * - 이미 읽은 글 제외
     * - 랜덤 시드를 사용하여 매번 다른 후보군 생성
     */
    private List<MmrCandidate> searchCandidates(float[] userProfileVector, User user) throws IOException {
        // 이미 읽은 글 ID 목록
        Set<Long> readPostIds = readPostRepository.findRecentReadPostsByUserIdWithMinDuration(user.getId(), PageRequest.of(0, 1000))
                .stream()
                .map(readPost -> readPost.getPost().getId())
                .collect(Collectors.toSet());

        log.debug("사용자 {}의 읽은 게시글 {} 개 제외", user.getId(), readPostIds.size());

        // 가중치 가져오기
        RecommendationProperties.EmbeddingWeights weights = properties.getEmbeddingWeights();

        // 1. 읽은 글 제외 필터 쿼리 생성 (Pre-filtering)
        Query filterQuery = createExcludeFilter(readPostIds);

        // 2. 네이티브 k-NN 검색 객체 리스트 생성 (Title + Summary + Content)
        List<KnnSearch> knnSearches = vectorQueryBuilder.createKnnSearches(
                TITLE_EMBEDDING_FIELD,
                SUMMARY_EMBEDDING_FIELD,
                CONTENT_CHUNKS_EMBEDDING_FIELD,
                userProfileVector,
                weights.getTitle(),
                weights.getSummary(),
                weights.getContent(),
                properties.getKnnSearchSize(),
                properties.getNumCandidates(),
                filterQuery
        );

        log.debug("ES k-NN 검색 실행 - 가중치 [title:{}, summary:{}, content:{}]",
                weights.getTitle(), weights.getSummary(), weights.getContent());

        long startTime = System.currentTimeMillis();
        SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                        .index(POSTS_INDEX)
                        .knn(knnSearches)       // k-NN 검색 (관련성 + 필터링)
                        .size(properties.getKnnSearchSize())
                ,
                PostDocument.class
        );
        long duration = System.currentTimeMillis() - startTime;
        log.info("추천 후보군 검색 완료: {} 개, 소요 시간: {}ms", response.hits().hits().size(), duration);

        // 결과를 MmrCandidate로 변환
        return response.hits().hits().stream()
                .filter(hit -> hit.source() != null)
                .map(this::mapToMmrCandidate)
                .filter(candidate -> candidate.getSummaryVector() != null)
                .toList();
    }

    /**
     * 읽은 글 제외를 위한 필터 쿼리 생성
     */
    private Query createExcludeFilter(Set<Long> readPostIds) {
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

    /**
     * PostDocument를 MmrCandidate로 변환
     * 시간 감쇠 가중치를 유사도 점수에 적용
     */
    private MmrCandidate mapToMmrCandidate(Hit<PostDocument> hit) {
        PostDocument doc = hit.source();
        double score = Objects.requireNonNullElse(hit.score(), 0.0);

        // 시간 감쇠 가중치 적용
        double timeDecayWeight = timeDecayStrategy.calculateWeight(Objects.requireNonNull(doc).getPublishedAt());
        double adjustedScore = score * timeDecayWeight;

        log.trace("게시글 {} 점수 조정: 원본={}, 시간가중치={}, 최종={}",
                doc.getPostId(), score, timeDecayWeight, adjustedScore);

        float[] titleVector = VectorUtil.convertToFloatArray(doc.getTitleEmbedding());
        float[] summaryVector = VectorUtil.convertToFloatArray(doc.getSummaryEmbedding());

        return MmrCandidate.builder()
                .postId(doc.getPostId())
                .titleVector(titleVector)
                .summaryVector(summaryVector)
                .similarityScore(adjustedScore)
                .build();
    }
}
