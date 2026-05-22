package com.techfork.post.domain;

import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

@Entity
@Table(name = "post_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostKeyword extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String keyword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @PersistenceCreator
    @Builder
    PostKeyword(String keyword, Post post) {
        this.keyword = keyword;
        this.post = post;
    }

    public static PostKeyword create(String keyword, Post post) {
        return PostKeyword.builder()
                .keyword(keyword)
                .post(post)
                .build();
    }
}
