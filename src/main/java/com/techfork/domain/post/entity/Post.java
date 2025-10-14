package com.techfork.domain.post.entity;

import com.techfork.domain.source.entity.TechBlog;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String fullContent;

    @Column(nullable = false)
    private String company;

    @Column(unique = true, nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime crawledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_blog_id", nullable = false)
    private TechBlog techBlog;
}
