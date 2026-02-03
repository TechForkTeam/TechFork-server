package com.techfork.domain.recommendation.setup.components;

import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostDocumentRepository;
import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.global.util.VectorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ground Truth 계산 및 품질 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroundTruthGenerator {

    private final PostDocumentRepository postDocumentRepository;
    private final PostMatcher postMatcher;

    /**
     * Ground Truth 관련도 점수 계산
     *
     * @param posts               평가할 게시글 목록
     * @param interests           사용자 관심사
     * @param userProfileVector   사용자 프로필 벡터 (임베딩)
     * @return 게시글 ID -> 관련도 점수 (1~5점)
     */
    public Map<Long, Integer> calculateGroundTruth(
            List<Post> posts,
            List<EInterestCategory> interests,
            float[] userProfileVector) {

        Map<Long, Integer> groundTruthScores = new HashMap<>();

        for (Post post : posts) {
            int score = calculateRelevanceScore(post, interests, userProfileVector);
            groundTruthScores.put(post.getId(), score);
        }

        return groundTruthScores;
    }

    /**
     * 게시글의 관련도 점수 계산 (1~5점)
     * 임베딩 유사도 기반 + 키워드 보조
     */
    private int calculateRelevanceScore(
            Post post,
            List<EInterestCategory> interests,
            float[] userProfileVector) {

        // 1. 임베딩 기반 유사도 계산 (우선순위)
        double embeddingSimilarity = 0.0;

        if (userProfileVector != null) {
            Optional<PostDocument> postDocOpt = postDocumentRepository.findByPostId(post.getId());
            if (postDocOpt.isPresent()) {
                PostDocument postDoc = postDocOpt.get();

                // Title과 Summary 임베딩 평균
                double titleSim = VectorUtil.cosineSimilarity(
                        userProfileVector,
                        postDoc.getTitleEmbedding()
                );
                double summarySim = VectorUtil.cosineSimilarity(
                        userProfileVector,
                        postDoc.getSummaryEmbedding()
                );

                embeddingSimilarity = (titleSim * 0.6 + summarySim * 0.4); // Title 가중치 더 높게

                log.debug("Post {}: 임베딩 유사도 = {} (title={}, summary={})",
                        post.getId(),
                        String.format("%.3f", embeddingSimilarity),
                        String.format("%.3f", titleSim),
                        String.format("%.3f", summarySim));
            }
        }

        // 2. 키워드 매칭 보조 점수
        int keywordMatches = countKeywordMatches(post, interests);

        // 3. 최종 점수 산정 (임베딩 우선, 키워드 보조)
        int score;

        if (embeddingSimilarity > 0.8 || keywordMatches >= 3) {
            score = 5; // 매우 관련있음
        } else if (embeddingSimilarity > 0.6 || keywordMatches >= 2) {
            score = 4; // 관련있음
        } else if (embeddingSimilarity > 0.4 || keywordMatches >= 1) {
            score = 3; // 보통
        } else if (embeddingSimilarity > 0.2) {
            score = 2; // 약간 관련
        } else {
            score = 1; // 거의 무관
        }

        return score;
    }

    /**
     * 키워드 매칭 개수 세기
     */
    private int countKeywordMatches(Post post, List<EInterestCategory> interests) {
        String title = post.getTitle().toLowerCase();
        String company = post.getCompany() != null ? post.getCompany().toLowerCase() : "";

        int matches = 0;

        for (EInterestCategory interest : interests) {
            List<String> keywords = postMatcher.getKeywordsForCategory(interest);

            for (String keyword : keywords) {
                String lowerKeyword = keyword.toLowerCase();
                if (title.contains(lowerKeyword) || company.contains(lowerKeyword)) {
                    matches++;
                    break; // 카테고리당 최대 1개만 카운트
                }
            }
        }

        return matches;
    }

    /**
     * Ground Truth 품질 검증
     * - 최소 3점 이상 글이 충분한지
     * - 점수 분포가 편향되지 않았는지
     */
    public void validateGroundTruthQuality(
            Map<Long, Integer> groundTruthScores,
            List<EInterestCategory> interests) {

        if (groundTruthScores.isEmpty()) {
            log.error("Ground Truth가 비어있습니다!");
            throw new IllegalStateException("Ground Truth가 비어있습니다.");
        }

        int totalCount = groundTruthScores.size();
        long highQualityCount = groundTruthScores.values().stream()
                .filter(score -> score >= 3)
                .count();

        double highQualityRatio = (double) highQualityCount / totalCount;

        log.info("===== Ground Truth 품질 검증 =====");
        log.info("총 개수: {}", totalCount);
        log.info("3점 이상: {} 개 ({}%)", highQualityCount, String.format("%.1f", highQualityRatio * 100));

        // 경고: 3점 이상이 50% 미만
        if (highQualityRatio < 0.5) {
            log.warn("⚠️ 경고: 3점 이상 비율이 낮습니다 ({}%). 관심사와 맞는 글이 부족할 수 있습니다.",
                    String.format("%.1f", highQualityRatio * 100));
            log.warn("관심사: {}", interests);
        }

        // 에러: 3점 이상이 20% 미만
        if (highQualityRatio < 0.2) {
            log.error("❌ Ground Truth 품질이 너무 낮습니다. 관심사와 맞는 게시글이 부족합니다.");
            throw new IllegalStateException(
                    String.format("Ground Truth 품질 불량: 3점 이상 비율 %.1f%% (최소 20%% 필요)",
                            highQualityRatio * 100));
        }

        // 최고 점수 확인
        int maxScore = groundTruthScores.values().stream()
                .max(Integer::compareTo)
                .orElse(0);

        if (maxScore < 3) {
            log.warn("⚠️ 경고: 최고 점수가 {}점입니다. 관련도 높은 글이 없을 수 있습니다.", maxScore);
        }

        log.info("✓ Ground Truth 품질 검증 통과");
    }
}
