package com.techfork.domain.post.batch;

import com.techfork.domain.post.document.PostDocument;
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

/**
 * PostDocument를 Elasticsearch에 Bulk Insert하는 Writer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEmbeddingWriter implements ItemWriter<PostDocument> {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void write(Chunk<? extends PostDocument> chunk) throws Exception {
        var documents = chunk.getItems();

        if (documents.isEmpty()) {
            return;
        }

        try {
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

        } catch (Exception e) {
            log.error("Elasticsearch Bulk Insert 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}
