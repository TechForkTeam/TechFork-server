package com.techfork.domain.post.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostSortType {
    LATEST("최신순"),
    POPULAR("인기순");

    private final String description;
}
