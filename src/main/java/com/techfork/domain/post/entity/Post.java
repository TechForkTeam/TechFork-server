package com.techfork.domain.post.entity;

import com.techfork.domain.source.dto.RssFeedItem;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.PersistenceCreator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_published_at_id", columnList = "publishedAt, id"),
        @Index(name = "idx_post_view_count_id", columnList = "viewCount, id"),
        @Index(name = "idx_post_company_published_at_id", columnList = "company, publishedAt, id")
})
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

    @Column(columnDefinition = "TEXT")
    private String shortSummary;

    @Column(nullable = false)
    private String company;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 1000)
    private String thumbnailUrl;

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
    @BatchSize(size = 100)
    private List<PostKeyword> keywords = new ArrayList<>();

    @PersistenceCreator
    @Builder
    Post(String title, String fullContent, String plainContent, String summary, String shortSummary, String company, String logoUrl, String thumbnailUrl,
                 String url, LocalDateTime publishedAt, LocalDateTime crawledAt, LocalDateTime embeddedAt, TechBlog techBlog) {
        this.title = title;
        this.fullContent = fullContent;
        this.plainContent = plainContent;
        this.summary = summary;
        this.shortSummary = shortSummary;
        this.company = company;
        this.logoUrl = logoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.url = url;
        this.publishedAt = publishedAt;
        this.crawledAt = crawledAt;
        this.embeddedAt = embeddedAt;
        this.techBlog = techBlog;
    }

    public static Post create(RssFeedItem item, TechBlog techBlog) {
        return Post.builder()
                .title(item.title())
                .fullContent(item.content())
                .plainContent(item.plainContent())
                .company(item.company())
                .logoUrl(item.logoUrl())
                .thumbnailUrl(item.thumbnailUrl())
                .url(item.url())
                .publishedAt(item.publishedAt())
                .crawledAt(LocalDateTime.now())
                .techBlog(techBlog)
                .build();
    }


    public void updateSummaries(String summary, String shortSummary) {
        this.summary = summary;
        this.shortSummary = shortSummary;
    }

    public void addKeyword(PostKeyword keyword) {
        this.keywords.add(keyword);
    }

    public void clearKeywords() {
        this.keywords.clear();
    }
}
