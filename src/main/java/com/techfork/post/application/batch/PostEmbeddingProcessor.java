package com.techfork.post.application.batch;

import com.techfork.post.domain.projection.ContentChunk;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.domain.Post;
import com.techfork.post.application.embedding.ContentChunkerService;
import com.techfork.global.llm.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEmbeddingProcessor implements ItemProcessor<Post, PostDocument> {

    private final ContentChunkerService contentChunkerService;
    private final EmbeddingClient embeddingClient;

    @Override
    public PostDocument process(Post post) {
        log.info("임베딩 처리 시작: Post ID={}, Title={}", post.getId(), post.getTitle());

        if (post.getTitle() == null || post.getTitle().isBlank()) {
            log.warn("Post ID={}의 제목이 비어있어 임베딩을 스킵합니다.", post.getId());
            return null;
        }
        if (post.getSummary() == null || post.getSummary().isBlank()) {
            log.warn("Post ID={}의 요약이 비어있어 임베딩을 스킵합니다.", post.getId());
            return null;
        }

        List<Float> titleEmbedding = embeddingClient.embed(post.getTitle());
        List<Float> summaryEmbedding = embeddingClient.embed(post.getSummary());

        List<String> rawChunks = contentChunkerService.chunkContent(post.getFullContent());

        List<String> validChunks = rawChunks.stream()
                .filter(chunk -> chunk != null && !chunk.isBlank())
                .toList();

        log.info("Post ID={} 청크 개수: (원본: {}, 유효: {})", post.getId(), rawChunks.size(), validChunks.size());

        if (validChunks.isEmpty()) {
            log.warn("Post ID={}의 본문에서 유효한 텍스트 청크를 찾을 수 없어 스킵합니다.", post.getId());
            return null;
        }

        List<List<Float>> chunkEmbeddings = embeddingClient.embedBatch(validChunks);

        List<ContentChunk> contentChunks = new ArrayList<>();
        for (int i = 0; i < validChunks.size(); i++) {
            ContentChunk chunk = ContentChunk.create(i, validChunks.get(i), chunkEmbeddings.get(i));
            contentChunks.add(chunk);
        }

        PostDocument postDocument = PostDocument.create(
                post,
                titleEmbedding,
                summaryEmbedding,
                contentChunks
        );

        log.info("임베딩 처리 완료: Post ID={}, Chunks={}", post.getId(), contentChunks.size());
        return postDocument;
    }
}