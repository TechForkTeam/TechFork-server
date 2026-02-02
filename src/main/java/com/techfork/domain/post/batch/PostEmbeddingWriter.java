package com.techfork.domain.post.batch;

import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEmbeddingWriter implements ItemWriter<PostDocument> {

    private final ElasticsearchOperations elasticsearchOperations;
    private final PostRepository postRepository;

    @Override
    public void write(Chunk<? extends PostDocument> chunk) throws Exception {
        var documents = chunk.getItems();

        if (documents.isEmpty()) {
            return;
        }

        List<IndexQuery> queries = documents.stream()
                .map(doc -> new IndexQueryBuilder()
                        .withId(doc.getId())
                        .withObject(doc)
                        .build())
                .toList();

        IndexCoordinates index = IndexCoordinates.of("posts");
        List<IndexedObjectInformation> result = elasticsearchOperations.bulkIndex(queries, index);

        log.info("Elasticsearch Bulk Insert 완료: {} 개 성공", result.size());

        if (result.size() != documents.size()) {
            log.warn("일부 문서 저장 실패: 요청={}, 성공={}", documents.size(), result.size());
        }

        List<Long> successPostIds = result.stream()
                .map(IndexedObjectInformation::id)
                .map(Long::parseLong)
                .toList();

        if (!successPostIds.isEmpty()) {
            postRepository.bulkUpdateEmbeddedAt(successPostIds, java.time.LocalDateTime.now());
            log.info("Post embeddedAt Bulk 업데이트 완료: {} 개", successPostIds.size());
        } else {
            log.warn("ES 저장에 성공한 문서가 없어 embeddedAt 업데이트를 건너뜁니다.");
        }
    }
}
