package com.techfork.domain.activity.readpost.entity;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

import java.time.LocalDateTime;

@Entity
@Table(name = "read_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadPost extends BaseEntity {

    @Column(nullable = false)
    private LocalDateTime readAt;

    private Integer readDurationSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @PersistenceCreator
    @Builder
    ReadPost(User user, Post post, LocalDateTime readAt, Integer readDurationSeconds) {
        this.user = user;
        this.post = post;
        this.readAt = readAt;
        this.readDurationSeconds = readDurationSeconds;
    }

    public static ReadPost create(User user, Post post, LocalDateTime readAt, Integer readDurationSeconds) {
        return ReadPost.builder()
                .user(user)
                .post(post)
                .readAt(readAt)
                .readDurationSeconds(readDurationSeconds)
                .build();
    }
}
