package com.techfork.domain.recommendation.service;

import com.techfork.useraccount.domain.User;
import java.util.List;

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

    /**
     * 사용자별 개인화 추천 게시글 생성
     *
     * <p>이미 생성된 개인화 프로필 스냅샷을 기반으로 추천을 생성합니다.
     * 프로필 저장 직후 이벤트 소비자가 Elasticsearch refresh 이전 검색 가시성에 의존하지 않도록 사용합니다.</p>
     *
     * @param user 추천 대상 사용자
     * @param personalizationProfileVector 개인화 프로필 벡터
     * @param keyKeywords 개인화 프로필 핵심 키워드
     * @return 생성된 추천 개수
     */
    int generateRecommendationsForUser(User user, float[] personalizationProfileVector, List<String> keyKeywords);
}
