package com.techfork.global.llm.impl;

import com.techfork.global.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenAI GPT 기반 LLM 클라이언트 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {

    private final OpenAiChatModel chatModel;

    @Override
    public String call(String systemPrompt, String userPrompt) {
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            ));

            String response = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("OpenAI API 응답: {}", response);
            return response;

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("LLM 호출 실패", e);
        }
    }
}
