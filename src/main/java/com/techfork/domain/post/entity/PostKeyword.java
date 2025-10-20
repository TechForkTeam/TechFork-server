package com.techfork.domain.post.entity;

import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Post and Keyword many-to-many relationship table
 */
@Entity
@Table(name = "post_keywords", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_keyword", columnNames = {"post_id", "keyword_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostKeyword extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Builder
    private PostKeyword(Post post, Keyword keyword) {
        this.post = post;
        this.keyword = keyword;
    }

    public static PostKeyword create(Post post, Keyword keyword) {
        return PostKeyword.builder()
                .post(post)
                .keyword(keyword)
                .build();
    }
}
