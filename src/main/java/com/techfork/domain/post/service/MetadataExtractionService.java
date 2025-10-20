package com.techfork.domain.post.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.domain.post.dto.ExtractedMetadata;
import com.techfork.global.util.ContentCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataExtractionService {

    private final AnthropicChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            너는 테크 블로그의 분석가야. 너는 테크 블로그들에서 메타 데이터들을 뽑아내는 역할을 맡았어.
            답변은 오직 JSON 형식으로만 주고, 추가적인 글이나 설명은 주지마.
            """;

    public ExtractedMetadata extractMetadata(String title, String content) {
        try {
            // 1. HTML/마크다운 정제 - 순수 텍스트만 추출
            String cleanedContent = ContentCleaner.clean(content);

            // 2. 길이 제한 (약 50,000자 = ~12,500 토큰)
            // Claude Haiku는 200K context window를 지원하지만 적정 크기로 제한
            String processedContent = cleanedContent;
            if (cleanedContent.length() > 50000) {
                processedContent = ContentCleaner.cleanAndLimit(content, 50000);
                log.debug("콘텐츠가 너무 길어 50,000자로 제한: {} (원본: {}자 -> 정제 후: {}자)",
                         title, content != null ? content.length() : 0, processedContent.length());
            }

            String userPrompt = buildUserPrompt(title, processedContent);

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(userPrompt)
            ));

            String response = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("Claude API 응답: {}", response);

            return parseJsonResponse(response);

        } catch (Exception e) {
            log.error("메타데이터 추출 실패: {}", e.getMessage(), e);
            return createDefaultMetadata();
        }
    }

    private String buildUserPrompt(String title, String content) {
        return String.format("""
                다음 기술 블로그를 분석해서 JSON으로 추출해줘:

                제목: %s
                내용: %s

                추출 항목:
                1. main_topics: 주요 주제 2-3개 (배열)
                2. tech_stack.languages: 프로그래밍 언어 목록 (배열)
                3. tech_stack.frameworks: 사용된 프레임워크 목록 (배열)
                4. tech_stack.tools: 데이터베이스, 인프라 도구 등 (배열)
                5. difficulty: beginner, intermediate, advanced 중 하나

                JSON만 반환. 형식:
                {
                  "main_topics": ["주제1", "주제2"],
                  "tech_stack": {
                    "languages": ["Java"],
                    "frameworks": ["Spring Boot"],
                    "tools": ["MySQL"]
                  },
                  "difficulty": "intermediate"
                }
                """, title, content);
    }

    private ExtractedMetadata parseJsonResponse(String response) {
        try {
            String cleanedResponse = response
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            return objectMapper.readValue(cleanedResponse, ExtractedMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", response, e);
            return createDefaultMetadata();
        }
    }

    private ExtractedMetadata createDefaultMetadata() {
        return new ExtractedMetadata(
                List.of(),
                new ExtractedMetadata.TechStack(List.of(), List.of(), List.of()),
                "intermediate"
        );
    }
}
