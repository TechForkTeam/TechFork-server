package com.techfork.domain.post.service;

import com.techfork.global.llm.LlmClient;
import com.techfork.global.util.ContentCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * LLM을 사용한 게시글 요약 추출 서비스
 * 의미 기반 검색 최적화를 위한 요약 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryExtractionService {

    private final LlmClient llmClient;

    private static final String SYSTEM_PROMPT = """
            너는 기술 블로그 요약 전문가야.
            주어진 기술 블로그 글을 분석하고, 의미 기반 검색에 최적화된 요약을 작성해줘.

            이 요약은 검색어와 함께 벡터 검색에 사용되어, 관련 글의 chunk를 찾는데 도움을 줘야 해.

            요약 작성 원칙:
            1. 글의 전체 맥락과 흐름을 포함 (300-500자)
            2. 글에서 다루는 핵심 문제와 해결 방법을 구체적으로 기술
            3. 사용된 기술 스택, 도구, 프레임워크를 정확한 명칭으로 명시
            4. 주요 개념과 키워드를 자연스럽게 포함 (검색 매칭용)
            5. 관련 검색어가 다양하게 매칭될 수 있도록 유사 표현 활용
            6. 자연스러운 한국어 문장으로 작성
            7. 마크다운이나 특수 기호 없이 순수 텍스트로만 작성
            """;

    public String extractSummary(String title, String content) {
        try {
            String processedContent = content;
            if (content != null && content.length() > 50000) {
                processedContent = ContentCleaner.cleanAndLimit(content, 50000);
                log.debug("콘텐츠가 너무 길어 50,000자로 제한: {} (원본: {}자 -> 정제 후: {}자)",
                        title, content.length(), processedContent.length());
            }

            String userPrompt = buildUserPrompt(title, processedContent);
            String response = llmClient.call(SYSTEM_PROMPT, userPrompt);

            log.debug("LLM API 응답 (제목: {}): {}", title, response);
            return response.trim();

        } catch (Exception e) {
            log.error("요약 추출 실패 (제목: {}): {}", title, e.getMessage(), e);
            return "";
        }
    }

    private String buildUserPrompt(String title, String content) {
        return String.format("""
                다음 기술 블로그 글을 간결하게 요약해줘:

                제목: %s
                내용: %s

                요약 작성 가이드:
                - 이 글이 다루는 핵심 주제와 문제
                - 제시된 해결 방법이나 기술적 접근
                - 주요 기술 스택이나 도구
                - 독자가 얻을 수 있는 인사이트

                자연스러운 문장으로 요약해줘.
                """, title, content != null ? content : "");
    }
}
