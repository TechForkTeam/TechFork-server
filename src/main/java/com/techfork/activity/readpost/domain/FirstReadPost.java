package com.techfork.activity.readpost.domain;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

@Entity
@Table(
        name = "first_read_posts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_first_read_posts_user_post", columnNames = {"user_id", "post_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FirstReadPost extends BaseEntity {

    @Column(name = "first_read_at", nullable = false)
    private LocalDateTime firstReadAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @PersistenceCreator
    @Builder
    FirstReadPost(User user, Post post, LocalDateTime firstReadAt) {
        this.user = user;
        this.post = post;
        this.firstReadAt = firstReadAt;
    }

    public static FirstReadPost create(User user, Post post, LocalDateTime firstReadAt) {
        return FirstReadPost.builder()
                .user(user)
                .post(post)
                .firstReadAt(firstReadAt)
                .build();
    }
}
