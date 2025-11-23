package com.techfork.domain.recommendation.entity;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.user.entity.User;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 추천 이력 엔티티
 * 과거 추천 기록을 보관하여 추천 품질 분석 및 개선에 활용
 */
@Entity
@Table(
        name = "recommendation_history",
        indexes = {
                @Index(name = "idx_user_recommended_at", columnList = "user_id, recommended_at DESC"),
                @Index(name = "idx_recommended_at", columnList = "recommended_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationHistory extends BaseEntity {

    @Column(nullable = false)
    private Double similarityScore;

    @Column(nullable = false)
    private Double mmrScore;

    @Column(nullable = false)
    private Integer rankOrder;

    @Column(nullable = false)
    private LocalDateTime recommendedAt;
    
    @Column(nullable = false)
    private Boolean isClicked = false;

    private LocalDateTime clickedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Builder
    private RecommendationHistory(User user, Post post, Double similarityScore,
                                   Double mmrScore, Integer rankOrder, LocalDateTime recommendedAt) {
        this.user = user;
        this.post = post;
        this.similarityScore = similarityScore;
        this.mmrScore = mmrScore;
        this.rankOrder = rankOrder;
        this.recommendedAt = recommendedAt;
        this.isClicked = false;
    }

    /**
     * RecommendedPost로부터 이력 생성
     */
    public static RecommendationHistory fromRecommendedPost(RecommendedPost recommendedPost) {
        return RecommendationHistory.builder()
                .user(recommendedPost.getUser())
                .post(recommendedPost.getPost())
                .similarityScore(recommendedPost.getSimilarityScore())
                .mmrScore(recommendedPost.getMmrScore())
                .rankOrder(recommendedPost.getRankOrder())
                .recommendedAt(recommendedPost.getRecommendedAt())
                .build();
    }

    /**
     * 클릭 기록
     */
    public void markAsisClicked() {
        this.isClicked = true;
        this.clickedAt = LocalDateTime.now();
    }
}
