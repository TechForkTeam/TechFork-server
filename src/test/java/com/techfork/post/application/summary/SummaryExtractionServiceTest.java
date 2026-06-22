package com.techfork.post.application.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.post.application.summary.SummaryExtractionResult;
import com.techfork.global.llm.LlmClient;
import com.techfork.global.llm.exception.LlmException;
import com.techfork.global.util.ContentCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SummaryExtractionServiceTest {

    @Mock
    private LlmClient llmClient;

    private SummaryExtractionService summaryExtractionService;

    @BeforeEach
    void setUp() {
        summaryExtractionService = new SummaryExtractionService(llmClient, new ObjectMapper());
    }

    @Nested
    @DisplayName("extractSummary")
    class ExtractSummary {

        @Test
        @DisplayName("LLM JSON 응답에서 summary, shortSummary, keywords를 파싱한다")
        void jsonResponse_ParsesSummaryShortSummaryAndKeywords() {
            given(llmClient.call(anyString(), anyString()))
                    .willReturn("""
                            {
                              "summary": "상세 요약",
                              "shortSummary": "짧은 요약",
                              "keywords": ["AI", "Batch"]
                            }
                            """);

            SummaryExtractionResult result = summaryExtractionService.extractSummary("제목", "본문");

            assertThat(result.summary()).isEqualTo("상세 요약");
            assertThat(result.shortSummary()).isEqualTo("짧은 요약");
            assertThat(result.keywords()).containsExactly("AI", "Batch");
        }

        @Test
        @DisplayName("shortSummary가 없으면 빈 문자열을 반환한다")
        void missingShortSummary_ReturnsEmptyString() {
            given(llmClient.call(anyString(), anyString()))
                    .willReturn("""
                            {
                              "summary": "상세 요약",
                              "keywords": ["AI"]
                            }
                            """);

            SummaryExtractionResult result = summaryExtractionService.extractSummary("제목", "본문");

            assertThat(result.summary()).isEqualTo("상세 요약");
            assertThat(result.shortSummary()).isEmpty();
            assertThat(result.keywords()).containsExactly("AI");
        }

        @Test
        @DisplayName("keywords가 없으면 빈 리스트를 반환한다")
        void missingKeywords_ReturnsEmptyList() {
            given(llmClient.call(anyString(), anyString()))
                    .willReturn("""
                            {
                              "summary": "상세 요약",
                              "shortSummary": "짧은 요약"
                            }
                            """);

            SummaryExtractionResult result = summaryExtractionService.extractSummary("제목", "본문");

            assertThat(result.summary()).isEqualTo("상세 요약");
            assertThat(result.shortSummary()).isEqualTo("짧은 요약");
            assertThat(result.keywords()).isEmpty();
        }

        @Test
        @DisplayName("keywords가 배열이 아니면 빈 리스트를 반환한다")
        void nonArrayKeywords_ReturnsEmptyList() {
            given(llmClient.call(anyString(), anyString()))
                    .willReturn("""
                            {
                              "summary": "상세 요약",
                              "shortSummary": "짧은 요약",
                              "keywords": "AI"
                            }
                            """);

            SummaryExtractionResult result = summaryExtractionService.extractSummary("제목", "본문");

            assertThat(result.summary()).isEqualTo("상세 요약");
            assertThat(result.shortSummary()).isEqualTo("짧은 요약");
            assertThat(result.keywords()).isEmpty();
        }

        @Test
        @DisplayName("유효하지 않은 JSON 응답이면 예외를 던진다")
        void invalidJson_ThrowsException() {
            given(llmClient.call(anyString(), anyString()))
                    .willReturn("not-json");

            assertThatThrownBy(() -> summaryExtractionService.extractSummary("제목", "본문"))
                    .isInstanceOf(LlmException.class)
                    .hasMessageContaining("LLM summary response parsing failed");
        }

        @Test
        @DisplayName("본문이 너무 길면 50000자로 정제 후 제한한 내용을 프롬프트에 사용한다")
        void bodyTooLong_UsesCleanedAndLimitedContent() {
            String longContent = "word ".repeat(15000) + "TRAILING_MARKER";
            String expectedContent = ContentCleaner.cleanAndLimit(longContent, 50000);
            given(llmClient.call(anyString(), anyString()))
                    .willReturn("""
                            {
                              "summary": "상세 요약",
                              "shortSummary": "짧은 요약",
                              "keywords": ["AI"]
                            }
                            """);

            summaryExtractionService.extractSummary("긴 본문 제목", longContent);

            ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
            verify(llmClient).call(anyString(), userPromptCaptor.capture());
            String userPrompt = userPromptCaptor.getValue();

            assertThat(expectedContent.length()).isLessThan(longContent.length());
            assertThat(userPrompt).contains("제목: 긴 본문 제목");
            assertThat(userPrompt).contains(expectedContent);
            assertThat(userPrompt).doesNotContain("TRAILING_MARKER");
        }
    }
}
