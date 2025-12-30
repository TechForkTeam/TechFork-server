package com.techfork.domain.post.entity;

import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String fullContent;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String plainContent;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private String company;

    @Column(length = 500)
    private String logoUrl;

    @Column(unique = true, nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime crawledAt;

    @Column
    private LocalDateTime embeddedAt;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_blog_id", nullable = false)
    private TechBlog techBlog;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostKeyword> keywords = new ArrayList<>();

    @Builder
    private Post(String title, String fullContent, String plainContent, String company, String logoUrl, String url,
                 LocalDateTime publishedAt, LocalDateTime crawledAt, TechBlog techBlog) {
        this.title = title;
        this.fullContent = fullContent;
        this.plainContent = plainContent;
        this.company = company;
        this.logoUrl = logoUrl;
        this.url = url;
        this.publishedAt = publishedAt;
        this.crawledAt = crawledAt;
        this.techBlog = techBlog;
    }

    public static Post create(RssFeedItem item, TechBlog techBlog) {
        return Post.builder()
                .title(item.title())
                .fullContent(item.content())
                .plainContent(item.plainContent())
                .company(item.company())
                .logoUrl(item.logoUrl())
                .url(item.url())
                .publishedAt(item.publishedAt())
                .crawledAt(LocalDateTime.now())
                .techBlog(techBlog)
                .build();
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }

    public void addKeyword(PostKeyword keyword) {
        this.keywords.add(keyword);
    }

    public void clearKeywords() {
        this.keywords.clear();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
