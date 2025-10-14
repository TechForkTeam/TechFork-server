package com.techfork.domain.recommendation.entity;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.user.entity.User;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommended_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendedPost extends BaseEntity {

    @Column(nullable = false)
    private Double similarityScore;

    private LocalDateTime recommendedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}
