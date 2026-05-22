package com.techfork.post.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EPostSortType {
    LATEST("최신순"),
    POPULAR("인기순");

    private final String description;
}
