package com.techfork.global.llm;

/**
 * LLM(Large Language Model) 클라이언트 인터페이스
 * GPT, Claude 등 다양한 LLM을 추상화하여 사용
 */
public interface LlmClient {

    /**
     * LLM에 프롬프트를 전송하고 응답을 받음
     *
     * @param systemPrompt 시스템 프롬프트 (역할 정의)
     * @param userPrompt 사용자 프롬프트 (실제 요청)
     * @return LLM 응답 텍스트
     */
    String call(String systemPrompt, String userPrompt);
}
