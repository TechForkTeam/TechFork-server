package com.techfork.domain.post.dto;

import com.techfork.domain.post.entity.Post;

/**
 * Post와 추출된 메타데이터를 함께 담는 DTO
 */
public record PostWithMetadata(
        Post post,
        ExtractedMetadata metadata
) {
    public static PostWithMetadata of(Post post, ExtractedMetadata metadata) {
        return new PostWithMetadata(post, metadata);
    }
}
