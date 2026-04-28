package com.techfork.domain.recommendation.service;

import com.techfork.domain.useraccount.entity.User;

/**
 * 추천 전략 인터페이스
 * 다양한 추천 알고리즘을 구현할 수 있도록 추상화
 */
public interface RecommendationService {

    /**
     * 사용자별 개인화 추천 게시글 생성
     *
     * @param user 추천 대상 사용자
     * @return 생성된 추천 개수
     */
    int generateRecommendationsForUser(User user);
}
