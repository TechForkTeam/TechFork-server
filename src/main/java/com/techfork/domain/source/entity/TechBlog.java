package com.techfork.domain.source.entity;

import com.techfork.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
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
}
