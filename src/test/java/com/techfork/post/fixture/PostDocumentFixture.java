package com.techfork.post.fixture;

import com.techfork.post.domain.Post;
import com.techfork.post.domain.projection.ContentChunk;
import com.techfork.post.domain.projection.PostDocument;

import java.time.LocalDateTime;
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

    public static PostDocument createPostDocument(
            Post post,
            List<Float> titleEmbedding,
            List<Float> summaryEmbedding,
            List<Float> contentChunkEmbedding
    ) {
        return createPostDocumentWithChunks(
                post,
                titleEmbedding,
                summaryEmbedding,
                List.of(ContentChunk.create(0, "chunk", contentChunkEmbedding))
        );
    }

    private static PostDocument createPostDocumentWithChunks(
            Post post,
            List<Float> titleEmbedding,
            List<Float> summaryEmbedding,
            List<ContentChunk> contentChunks
    ) {
        return PostDocument.create(post, titleEmbedding, summaryEmbedding, contentChunks);
    }

    public static PostDocument createPostDocument(
            Long postId,
            String title,
            List<Float> titleEmbedding,
            List<Float> summaryEmbedding,
            LocalDateTime publishedAt
    ) {
        return PostDocument.builder()
                .id(String.valueOf(postId))
                .postId(postId)
                .title(title)
                .summary("배치 처리 요약")
                .shortSummary("짧은 요약")
                .company("TechFork")
                .url("https://posts.example.com/" + postId)
                .logoUrl("https://cdn.example.com/logo.png")
                .thumbnailUrl("https://cdn.example.com/thumb-" + postId + ".png")
                .publishedAtString(publishedAt.toString())
                .titleEmbedding(titleEmbedding)
                .summaryEmbedding(summaryEmbedding)
                .contentChunks(List.of())
                .build();
    }
}
