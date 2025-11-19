package com.techfork.domain.post.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.domain.post.dto.SummaryWithKeywordsDto;
import com.techfork.global.llm.LlmClient;
import com.techfork.global.util.ContentCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM을 사용한 게시글 요약 추출 서비스
 * 의미 기반 검색 최적화를 위한 요약 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryExtractionService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            너는 기술 블로그 분석 전문가야.
            주어진 기술 블로그 글을 분석하고, 요약과 핵심 키워드를 추출해줘.

            응답은 반드시 아래 JSON 형식으로만 작성해:
            {
              "summary": "요약 내용 (300-500자)",
              "keywords": ["키워드1", "키워드2", ...]
            }

            요약 작성 원칙:
            1. 글의 전체 맥락과 흐름을 포함 (300-500자)
            2. 글에서 다루는 핵심 문제와 해결 방법을 구체적으로 기술
            3. 사용된 기술 스택, 도구, 프레임워크를 정확한 명칭으로 명시
            4. 자연스러운 한국어 문장으로 작성
            5. 마크다운이나 특수 기호 없이 순수 텍스트로만 작성

            키워드 추출 원칙:
            1. 10-15개의 핵심 키워드 추출
            2. 기술 스택 (예: Spring Boot, React, Kubernetes)
            3. 주제/개념 (예: 성능 최적화, 마이크로서비스, CI/CD)
            4. 방법론 (예: TDD, DDD, 애자일)
            5. 영문과 한글 키워드 모두 포함
            6. 너무 일반적인 키워드(예: "개발", "코딩") 제외
            """;

    public SummaryWithKeywordsDto extractSummary(String title, String content) {
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

            // JSON 응답 파싱
            return parseResponse(response.trim());

        } catch (Exception e) {
            log.error("요약 추출 실패 (제목: {}): {}", title, e.getMessage(), e);
            return new SummaryWithKeywordsDto("", List.of());
        }
    }

    private SummaryWithKeywordsDto parseResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String summary = jsonNode.get("summary").asText();
            List<String> keywords = new ArrayList<>();

            JsonNode keywordsNode = jsonNode.get("keywords");
            if (keywordsNode != null && keywordsNode.isArray()) {
                keywordsNode.forEach(node -> keywords.add(node.asText()));
            }

            return new SummaryWithKeywordsDto(summary, keywords);
        } catch (Exception e) {
            log.error("JSON 응답 파싱 실패: {}", response, e);
            return new SummaryWithKeywordsDto("", List.of());
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
