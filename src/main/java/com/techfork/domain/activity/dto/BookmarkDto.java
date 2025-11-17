package com.techfork.domain.activity.dto;

import java.time.LocalDateTime;

public record BookmarkDto(
        Long bookmarkId,
        Long postId,
        String title,
        String url,
        String companyName,
        String logoUrl,
        LocalDateTime publishedAt
) {
}
