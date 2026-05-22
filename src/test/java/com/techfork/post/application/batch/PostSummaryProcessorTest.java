package com.techfork.post.application.batch;

import com.techfork.post.application.summary.SummaryExtractionResult;
import com.techfork.post.domain.Post;
import com.techfork.post.domain.PostKeyword;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.application.summary.SummaryExtractionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostSummaryProcessorTest {

    @Mock
    private SummaryExtractionService summaryExtractionService;

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("추출 결과로 summary를 갱신하고 keyword를 재구성한다")
        void updatesSummariesAndRebuildsKeywordsFromExtractionResult() {
            PostSummaryProcessor postSummaryProcessor = new PostSummaryProcessor(summaryExtractionService);
            Post post = createPostWithExistingKeywords();
            PostKeyword oldKeyword1 = post.getKeywords().get(0);
            PostKeyword oldKeyword2 = post.getKeywords().get(1);
            SummaryExtractionResult summaryWithKeywordsDto = new SummaryExtractionResult(
                    "새 요약",
                    "새 짧은 요약",
                    List.of("AI", "Batch")
            );
            given(summaryExtractionService.extractSummary("요약 대상 글", "평문 본문"))
                    .willReturn(summaryWithKeywordsDto);

            Post result = postSummaryProcessor.process(post);

            assertThat(result).isSameAs(post);
            assertThat(post.getSummary()).isEqualTo("새 요약");
            assertThat(post.getShortSummary()).isEqualTo("새 짧은 요약");
            assertThat(post.getKeywords()).doesNotContain(oldKeyword1, oldKeyword2);
            assertThat(post.getKeywords())
                    .extracting(PostKeyword::getKeyword)
                    .containsExactlyInAnyOrder("AI", "Batch");
            assertThat(post.getKeywords())
                    .allSatisfy(keyword -> assertThat(keyword.getPost()).isSameAs(post));
            verify(summaryExtractionService).extractSummary("요약 대상 글", "평문 본문");
        }

        @Test
        @DisplayName("추출 결과 keyword가 비어 있으면 기존 keyword를 모두 제거한다")
        void clearsExistingKeywordsWhenExtractionReturnsNoKeywords() {
            PostSummaryProcessor postSummaryProcessor = new PostSummaryProcessor(summaryExtractionService);
            Post post = createPostWithExistingKeywords();
            given(summaryExtractionService.extractSummary("요약 대상 글", "평문 본문"))
                    .willReturn(new SummaryExtractionResult("새 요약", "새 짧은 요약", List.of()));

            Post result = postSummaryProcessor.process(post);

            assertThat(result).isSameAs(post);
            assertThat(post.getSummary()).isEqualTo("새 요약");
            assertThat(post.getShortSummary()).isEqualTo("새 짧은 요약");
            assertThat(post.getKeywords()).isEmpty();
        }

        @Test
        @DisplayName("요약 추출 서비스 예외를 그대로 전파한다")
        void propagatesExtractionServiceFailure() {
            PostSummaryProcessor postSummaryProcessor = new PostSummaryProcessor(summaryExtractionService);
            Post post = createPostWithExistingKeywords();
            PostKeyword oldKeyword1 = post.getKeywords().get(0);
            PostKeyword oldKeyword2 = post.getKeywords().get(1);
            given(summaryExtractionService.extractSummary("요약 대상 글", "평문 본문"))
                    .willThrow(new IllegalStateException("LLM error"));

            assertThatThrownBy(() -> postSummaryProcessor.process(post))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("LLM error");
            assertThat(post.getSummary()).isEqualTo("기존 요약");
            assertThat(post.getShortSummary()).isEqualTo("기존 짧은 요약");
            assertThat(post.getKeywords()).containsExactly(oldKeyword1, oldKeyword2);
        }

        private Post createPostWithExistingKeywords() {
            return PostFixture.createPostWithKeywords(
                    1L,
                    "요약 대상 글",
                    "원문 본문",
                    "평문 본문",
                    "TechFork",
                    "기존 요약",
                    "기존 짧은 요약",
                    List.of("기존키워드1", "기존키워드2")
            );
        }
    }
}
