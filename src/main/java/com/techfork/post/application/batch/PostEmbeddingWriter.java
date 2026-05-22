package com.techfork.post.application.batch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.techfork.post.domain.projection.PostDocument;
import com.techfork.post.infrastructure.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEmbeddingWriter implements ItemWriter<PostDocument> {

    private final ElasticsearchClient elasticsearchClient;
    private final PostRepository postRepository;

    @Override
    public void write(Chunk<? extends PostDocument> chunk) throws Exception {
        var documents = chunk.getItems();

        if (documents.isEmpty()) {
            return;
        }

        BulkResponse bulkResponse = elasticsearchClient.bulk(b -> b
                .index("posts")
                .operations(documents.stream()
                        .map(doc -> BulkOperation.of(op -> op
                                .index(i -> i
                                        .id(doc.getId())
                                        .document(doc)
                                )
                        ))
                        .toList()
                )
        );

        if (bulkResponse == null) {
            log.error("Bulk 응답이 null입니다.");
            return;
        }

        if (bulkResponse.errors()) {
            log.warn("Bulk 인덱싱 중 일부 실패 발생");
        }

        List<Long> successPostIds = bulkResponse.items().stream()
                .filter(item -> item.error() == null)  // 에러 없는 것만
                .map(item -> Long.parseLong(item.id()))
                .toList();

        int failureCount = documents.size() - successPostIds.size();

        log.info("Elasticsearch Bulk Insert 완료: 성공={}, 실패={}", successPostIds.size(), failureCount);

        if (failureCount > 0) {
            log.warn("실패한 문서 ID 목록:");
            bulkResponse.items().stream()
                    .filter(item -> item.error() != null)
                    .forEach(item -> log.warn("ID={}, Error={}", item.id(), item.error().reason()));
        }

        if (!successPostIds.isEmpty()) {
            postRepository.bulkUpdateEmbeddedAt(successPostIds, java.time.LocalDateTime.now());
            log.info("Post embeddedAt Bulk 업데이트 완료: {} 개", successPostIds.size());
        } else {
            log.warn("ES 저장에 성공한 문서가 없어 embeddedAt 업데이트를 건너뜁니다.");
        }
    }
}
