package com.techfork.domain.post.batch;

import com.techfork.domain.post.document.ContentChunk;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.service.ContentChunkerService;
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
    public PostDocument process(Post post) throws Exception {
        log.info("임베딩 처리 시작: Post ID={}, Title={}", post.getId(), post.getTitle());

        try {
            List<Float> titleEmbedding = embeddingClient.embed(post.getTitle());
            List<Float> summaryEmbedding = embeddingClient.embed(post.getSummary());

            List<String> chunks = contentChunkerService.chunkContent(post.getFullContent());
            log.info("Post ID={} 청크 개수: {}", post.getId(), chunks.size());

            List<List<Float>> chunkEmbeddings = embeddingClient.embedBatch(chunks);

            List<ContentChunk> contentChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                ContentChunk chunk = ContentChunk.create(i, chunks.get(i), chunkEmbeddings.get(i));
                contentChunks.add(chunk);
            }

            PostDocument postDocument = PostDocument.create(
                    post,
                    titleEmbedding,
                    summaryEmbedding,
                    contentChunks
            );

            post.updateEmbedded();

            log.info("임베딩 처리 완료: Post ID={}, Chunks={}", post.getId(), contentChunks.size());
            return postDocument;

        } catch (Exception e) {
            log.error("임베딩 처리 실패: Post ID={}, Error={}", post.getId(), e.getMessage(), e);
            throw e;
        }
    }
}
