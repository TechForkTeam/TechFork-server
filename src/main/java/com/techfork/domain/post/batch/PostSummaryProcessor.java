package com.techfork.domain.post.batch;

import com.techfork.domain.post.dto.SummaryWithKeywords;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.service.SummaryExtractionService;
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
        try {
            log.debug("요약 및 키워드 추출 중: {}", post.getTitle());

            SummaryWithKeywords result = summaryExtractionService.extractSummary(
                    post.getTitle(),
                    post.getPlainContent()
            );

            // 요약 업데이트
            post.updateSummary(result.summary());

            // 기존 키워드 삭제 후 새 키워드 추가
            post.clearKeywords();
            result.keywords().forEach(keyword -> {
                PostKeyword postKeyword = PostKeyword.create(keyword, post);
                post.addKeyword(postKeyword);
            });

            log.debug("요약 및 키워드 추출 완료: {} (키워드 {}개)", post.getTitle(), result.keywords().size());
            return post;

        } catch (Exception e) {
            log.error("요약 및 키워드 추출 실패 (Post ID: {}): {}", post.getId(), e.getMessage(), e);
            post.updateSummary("");
            return post;
        }
    }
}
