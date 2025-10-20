package com.techfork.domain.post.entity;

import com.techfork.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Keyword entity
 */
@Entity
@Table(name = "keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword extends BaseEntity {

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Builder
    private Keyword(String name) {
        this.name = name;
    }

    public static Keyword create(String name) {
        return Keyword.builder()
                .name(name)
                .build();
    }
}
