package com.techfork.post.application.query;

import java.time.LocalDateTime;
import java.util.List;

public record GetPostsByCompanyV2Query(
        List<String> companies,
        LocalDateTime lastPublishedAt,
        Long lastPostId,
        int size,
        Long userId
) {
}
