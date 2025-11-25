package com.techfork.domain.recommendation_quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Train/Test Split 기반 개선된 추천 시스템 테스트 케이스
 *
 * 기존 방식의 문제:
 * 1. Ground Truth가 문자열 매칭 기반 (추천 시스템은 벡터 유사도 기반)
 * 2. Recall 분모가 너무 커서(100개) 지표가 낮게 나옴
 *
 * 개선 방식:
 * 1. 읽은 글을 8:2로 분할 (Train/Test)
 * 2. Test Set을 Ground Truth로 사용 (실제로 읽은 글 = 관심있는 글)
 * 3. 적절한 Recall 분모 (Test Set 크기 = 20개 정도)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImprovedRecommendationTestCase {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 사용자 관심사
     */
    private List<String> interests;

    /**
     * Train/Test 분할 결과
     */
    private TrainTestSplit trainTestSplit;

    /**
     * Test Set을 Ground Truth로 반환 (Recall 계산용)
     */
    public Set<Long> getGroundTruthPostIds() {
        return trainTestSplit.getTestPostIds().stream()
                .collect(Collectors.toSet());
    }

    /**
     * Train Set 반환 (사용자 프로필 생성용)
     */
    public List<Long> getTrainPostIds() {
        return trainTestSplit.getTrainPostIds();
    }

    /**
     * Test Set 반환 (평가용)
     */
    public List<Long> getTestPostIds() {
        return trainTestSplit.getTestPostIds();
    }
}
