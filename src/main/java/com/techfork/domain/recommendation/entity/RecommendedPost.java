package com.techfork.domain.recommendation.entity;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "recommended_posts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "post_id"})
        },
        indexes = {
                @Index(name = "idx_user_recommended_at", columnList = "user_id, recommended_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendedPost extends BaseEntity {

    @Column(nullable = false)
    private Double similarityScore;

    @Column(nullable = false)
    private Double mmrScore;

    @Column(nullable = false)
    private Integer rankOrder;

    @Column(nullable = false)
    private LocalDateTime recommendedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @PersistenceCreator
    @Builder
    RecommendedPost(User user, Post post, Double similarityScore,
                           Double mmrScore, Integer rankOrder, LocalDateTime recommendedAt) {
        this.user = user;
        this.post = post;
        this.similarityScore = similarityScore;
        this.mmrScore = mmrScore;
        this.rankOrder = rankOrder;
        this.recommendedAt = recommendedAt;
    }

    public static RecommendedPost create(User user, Post post, Double similarityScore,
                                        Double mmrScore, Integer rankOrder) {
        return RecommendedPost.builder()
                .user(user)
                .post(post)
                .similarityScore(similarityScore)
                .mmrScore(mmrScore)
                .rankOrder(rankOrder)
                .recommendedAt(LocalDateTime.now())
                .build();
    }
}
