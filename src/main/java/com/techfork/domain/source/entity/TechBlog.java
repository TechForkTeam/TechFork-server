package com.techfork.domain.source.entity;

import com.techfork.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tech_blogs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechBlog extends BaseTimeEntity {

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(unique = true, nullable = false, length = 500)
    private String blogUrl;

    @Column(unique = true, nullable = false, length = 500)
    private String rssUrl;

    @Column(length = 500)
    private String logoUrl;

    private LocalDateTime lastCrawledAt;

    @Builder
    private TechBlog(String companyName, String blogUrl, String rssUrl, String logoUrl) {
        this.companyName = companyName;
        this.blogUrl = blogUrl;
        this.rssUrl = rssUrl;
        this.logoUrl = logoUrl;
    }

    public static TechBlog create(String companyName, String blogUrl, String rssUrl) {
        return TechBlog.builder()
                .companyName(companyName)
                .blogUrl(blogUrl)
                .rssUrl(rssUrl)
                .build();
    }
}
