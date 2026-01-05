package com.techfork.global.llm.impl;

import com.techfork.global.llm.LlmClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Anthropic Claude 기반 LLM 클라이언트 구현체
 * Resilience4j를 통한 Circuit Breaker, Rate Limiter, Retry 패턴 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnthropicLlmClient implements LlmClient {

    private final AnthropicChatModel chatModel;

    @Override
    @Retry(name = "llmApi")
    @CircuitBreaker(name = "llmApi")
    @RateLimiter(name = "llmApi")
    public String call(String systemPrompt, String userPrompt) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        ));
        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
