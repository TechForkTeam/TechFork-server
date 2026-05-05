package com.techfork.activity.bookmark.entity;

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
        name = "bookmarks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bookmarks_user_post", columnNames = {"user_id", "post_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseEntity {

    @Column(name = "bookmarked_at")
    private LocalDateTime bookmarkedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @PersistenceCreator
    @Builder
    Bookmark(User user, Post post, LocalDateTime bookmarkedAt) {
        this.user = user;
        this.post = post;
        this.bookmarkedAt = bookmarkedAt;
    }

    public static Bookmark create(User user, Post post, LocalDateTime bookmarkedAt) {
        return Bookmark.builder()
                .user(user)
                .post(post)
                .bookmarkedAt(bookmarkedAt)
                .build();
    }
}
