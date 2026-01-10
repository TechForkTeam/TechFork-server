package com.techfork.domain.source.entity;

import com.techfork.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

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

    @PersistenceCreator
    @Builder
    TechBlog(String companyName, String blogUrl, String rssUrl,
                     String logoUrl, LocalDateTime lastCrawledAt) {
        this.companyName = companyName;
        this.blogUrl = blogUrl;
        this.rssUrl = rssUrl;
        this.lastCrawledAt = lastCrawledAt;

        if (logoUrl != null) {
            this.logoUrl = logoUrl;
        } else {
            this.logoUrl = String.format("https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=%s/&size=40", this.blogUrl);
        }
    }

    public static TechBlog create(String companyName, String blogUrl, String rssUrl, String logoUrl) {
        return TechBlog.builder()
                .companyName(companyName)
                .blogUrl(blogUrl)
                .rssUrl(rssUrl)
                .logoUrl(logoUrl)
                .build();
    }

}
