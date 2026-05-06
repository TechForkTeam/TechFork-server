package com.techfork.activity.readpost.presentation;

import com.techfork.activity.readpost.application.query.GetReadPostsResult;
import com.techfork.activity.readpost.application.query.ReadPostItem;
import org.springframework.stereotype.Component;

@Component
public class ReadPostConverter {

    public ReadPostListResponse toReadPostListResponse(GetReadPostsResult result) {
        return ReadPostListResponse.builder()
                .readPosts(result.readPosts().stream()
                        .map(this::toReadPostResponse)
                        .toList())
                .lastReadPostId(result.lastReadPostId())
                .hasNext(result.hasNext())
                .build();
    }

    private ReadPostListResponse.Item toReadPostResponse(ReadPostItem readPost) {
        return ReadPostListResponse.Item.builder()
                .readPostId(readPost.readPostId())
                .postId(readPost.postId())
                .title(readPost.title())
                .shortSummary(readPost.shortSummary())
                .url(readPost.url())
                .companyName(readPost.companyName())
                .logoUrl(readPost.logoUrl())
                .publishedAt(readPost.publishedAt())
                .thumbnailUrl(readPost.thumbnailUrl())
                .viewCount(readPost.viewCount())
                .keywords(readPost.keywords())
                .isBookmarked(readPost.isBookmarked())
                .readAt(readPost.readAt())
                .build();
    }
}
