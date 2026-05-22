package com.techfork.post.application.batch;

import com.techfork.post.application.dto.SummaryWithKeywordsDto;
import com.techfork.post.domain.Post;
import com.techfork.post.application.summary.SummaryExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Post에서 LLM을 사용하여 요약과 키워드를 추출하고 Post 엔티티에 저장하는 Processor
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PostSummaryProcessor implements ItemProcessor<Post, Post> {

    private final SummaryExtractionService summaryExtractionService;

    @Override
    public Post process(Post post) {
        log.debug("요약 및 키워드 추출 중: {}", post.getTitle());

        SummaryWithKeywordsDto result = summaryExtractionService.extractSummary(
                post.getTitle(),
                post.getPlainContent()
        );

        post.updateSummaries(result.summary(), result.shortSummary());
        post.replaceKeywords(result.keywords());

        log.debug("요약 및 키워드 추출 완료: {} (키워드 {}개)", post.getTitle(), result.keywords().size());
        return post;
    }
}
