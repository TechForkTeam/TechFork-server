package com.techfork.domain.recommendation.dto;

import com.techfork.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 테스트 사용자 생성 결과 (Leave-K-Out 방식)
 */
@Getter
@Builder
public class UserCreationResult {
    /**
     * 생성된 사용자
     */
    private User user;

    /**
     * Ground Truth 관련도 점수 (postId -> relevance score)
     * nDCG 계산을 위한 graded relevance (1~5점)
     * - 5: 매우 관련 높음 (키워드 3개 이상 매칭)
     * - 4: 관련 높음 (키워드 2개 매칭)
     * - 3: 중간 (키워드 1개 매칭)
     * - 2: 낮음 (유명 기업)
     * - 1: 기본
     */
    private Map<Long, Integer> groundTruthScores;
}