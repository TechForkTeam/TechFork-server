package com.techfork.global.llm.exception;

/**
 * LLM API 호출 중 발생하는 일반 예외
 * 서킷 브레이커에 카운트되어 실패율에 영향을 줌
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
