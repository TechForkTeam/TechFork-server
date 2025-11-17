package com.techfork.domain.activity.entity;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.user.entity.User;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "scrap_posts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "post_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScrabPost extends BaseEntity {

    private LocalDateTime scrappedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Builder
    private ScrabPost(User user, Post post, LocalDateTime scrappedAt) {
        this.user = user;
        this.post = post;
        this.scrappedAt = scrappedAt;
    }

    public static ScrabPost create(User user, Post post, LocalDateTime scrappedAt) {
        return ScrabPost.builder()
                .user(user)
                .post(post)
                .scrappedAt(scrappedAt)
                .build();
    }
}
