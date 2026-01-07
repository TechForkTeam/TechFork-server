package com.techfork.global.llm.exception;

/**
 * LLM API 네트워크 연결 예외
 * Timeout, Connection Refused 등 네트워크 문제 발생 시
 * 서킷 브레이커에서 무시되지만 재시도는 수행됨
 */
public class LlmNetworkException extends RuntimeException {

    public LlmNetworkException(String message) {
        super(message);
    }

    public LlmNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
