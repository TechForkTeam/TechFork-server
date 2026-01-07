package com.techfork.global.llm.exception;

/**
 * LLM API Rate Limit 초과 예외
 * 429 Too Many Requests 응답 시 발생
 * 서킷 브레이커에서 무시되어 서킷이 열리지 않음
 */
public class LlmRateLimitException extends RuntimeException {

    public LlmRateLimitException(String message) {
        super(message);
    }

    public LlmRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
