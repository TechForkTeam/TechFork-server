package com.techfork.domain.post.batch;

import com.techfork.domain.post.dto.ExtractedMetadata;
import com.techfork.domain.post.dto.PostWithMetadata;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.service.MetadataExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostMetadataProcessor implements ItemProcessor<Post, PostWithMetadata> {

    private final MetadataExtractionService metadataExtractionService;

    @Override
    public PostWithMetadata process(Post post) {
        try {
            log.debug("메타데이터 추출 중: {}", post.getTitle());

            ExtractedMetadata metadata = metadataExtractionService.extractMetadata(
                    post.getTitle(),
                    post.getFullContent()
            );

            return PostWithMetadata.of(post, metadata);

        } catch (Exception e) {
            log.error("메타데이터 추출 실패 (Post ID: {}): {}", post.getId(), e.getMessage(), e);
            // 실패해도 기본 메타데이터로 계속 진행
            return PostWithMetadata.of(post, createDefaultMetadata());
        }
    }

    private ExtractedMetadata createDefaultMetadata() {
        return new ExtractedMetadata(
                java.util.List.of(),
                new ExtractedMetadata.TechStack(java.util.List.of(), java.util.List.of(), java.util.List.of()),
                "intermediate"
        );
    }
}
