package com.techfork.domain.post.batch;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.service.SummaryExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Post에서 LLM을 사용하여 요약을 추출하고 Post 엔티티에 저장하는 Processor
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostSummaryProcessor implements ItemProcessor<Post, Post> {

    private final SummaryExtractionService summaryExtractionService;

    @Override
    public Post process(Post post) {
        try {
            log.debug("요약 추출 중: {}", post.getTitle());

            String summary = summaryExtractionService.extractSummary(
                    post.getTitle(),
                    post.getPlainContent()
            );

            post.updateSummary(summary);
            return post;

        } catch (Exception e) {
            log.error("요약 추출 실패 (Post ID: {}): {}", post.getId(), e.getMessage(), e);
            post.updateSummary("");
            return post;
        }
    }
}
