package com.techfork.global.util;

import java.time.LocalDateTime;

/**
 * 시간 감쇠 전략 인터페이스
 * 게시글의 발행 시간에 따라 가중치를 계산하는 다양한 전략을 정의
 */
public interface TimeDecayStrategy {

    /**
     * 게시글의 발행일 기준 시간 감쇠 가중치 계산
     *
     * @param publishedAt 게시글 발행일
     * @return 시간 감쇠 가중치 (일반적으로 0.0 ~ 2.0 사이)
     */
    double calculateWeight(LocalDateTime publishedAt);
}
