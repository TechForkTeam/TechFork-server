package com.techfork.post.fixture;

import com.techfork.post.domain.Post;
import com.techfork.post.domain.projection.ContentChunk;
import com.techfork.post.domain.projection.PostDocument;

import java.util.List;

public final class PostDocumentFixture {

    private PostDocumentFixture() {
    }

    public static PostDocument createPostDocument(Long id) {
        Post post = PostFixture.createPost(
                id,
                "임베딩 대상 글 " + id,
                "원문 본문 " + id,
                "평문 본문 " + id,
                "TechFork",
                "요약 완료 " + id,
                "짧은 요약 " + id
        );
        return PostDocument.create(
                post,
                List.of(0.1f, 0.2f),
                List.of(0.3f, 0.4f),
                List.of(ContentChunk.create(0, "chunk-" + id, List.of(0.5f, 0.6f)))
        );
    }
}
