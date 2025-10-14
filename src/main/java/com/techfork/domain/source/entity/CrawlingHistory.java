package com.techfork.domain.source.entity;

import com.techfork.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crawling_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlingHistory extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_blog_id", nullable = false)
    private TechBlog techBlog;
}
