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
     * 임베딩 유사도 우선 + 키워드 보너스
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

        // 3. 최종 점수 산정 (임베딩 기반, 키워드는 보너스)
        int baseScore;

        // 임베딩 유사도로 기본 점수 결정 (실제 데이터 분포에 맞춘 구간)
        // 일반적으로 cosine similarity는 0.15~0.45 범위에 분포
        if (embeddingSimilarity >= 0.40) {
            baseScore = 5; // 매우 관련있음
        } else if (embeddingSimilarity >= 0.35) {
            baseScore = 4; // 관련있음
        } else if (embeddingSimilarity >= 0.28) {
            baseScore = 3; // 보통
        } else if (embeddingSimilarity >= 0.22) {
            baseScore = 2; // 약간 관련
        } else {
            baseScore = 1; // 거의 무관
        }

        // 키워드 보너스 (최대 +1점)
        int bonusScore = 0;
        if (keywordMatches >= 2) {
            bonusScore = 1;
        }

        int finalScore = Math.min(5, baseScore + bonusScore);

        log.debug("Post {}: 최종 점수 = {} (임베딩 기본={}, 키워드 보너스={}, 매칭수={})",
                post.getId(), finalScore, baseScore, bonusScore, keywordMatches);

        return finalScore;
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
}
