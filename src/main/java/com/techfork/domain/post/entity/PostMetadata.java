package com.techfork.domain.post.entity;

import com.techfork.domain.post.enums.EDifficultyLevel;
import com.techfork.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMetadata extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EDifficultyLevel difficulty;

    @Column(length = 1000)
    private String languages;  // 쉼표로 구분된 언어 목록

    @Column(length = 1000)
    private String frameworks;  // 쉼표로 구분된 프레임워크 목록

    @Column(length = 1000)
    private String tools;  // 쉼표로 구분된 도구 목록

    @Column(columnDefinition = "TEXT")
    private String mainTopics;  // 쉼표로 구분된 주요 주제 목록

    @Builder
    private PostMetadata(Post post, EDifficultyLevel difficulty, String languages,
                         String frameworks, String tools, String mainTopics) {
        this.post = post;
        this.difficulty = difficulty;
        this.languages = languages;
        this.frameworks = frameworks;
        this.tools = tools;
        this.mainTopics = mainTopics;
    }

    public static PostMetadata create(Post post, EDifficultyLevel difficulty,
                                       String languages, String frameworks,
                                       String tools, String mainTopics) {
        return PostMetadata.builder()
                .post(post)
                .difficulty(difficulty)
                .languages(languages)
                .frameworks(frameworks)
                .tools(tools)
                .mainTopics(mainTopics)
                .build();
    }
}
