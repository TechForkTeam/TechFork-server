package com.techfork.domain.recommendation_quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추천 시스템 평가를 위한 테스트 케이스
 * 단순 구조: 사용자 + 읽은 글 + Ground Truth
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationTestCase {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 사용자 관심사
     */
    private List<String> interests;

    /**
     * 읽은 글 이력 (사용자 프로필 벡터 생성에 사용)
     */
    private List<Long> readPostIds;

    /**
     * Ground Truth: 실제로 관심있을 만한 게시글과 관련도
     * 관련도 점수: 5(매우 관련), 4(관련), 3(보통), 2(조금), 1(약간)
     */
    private Map<Long, Integer> groundTruthScores;

    /**
     * Ground Truth에서 관련도 1 이상인 게시글 ID (Recall 계산용)
     */
    public Set<Long> getRelevantPostIds() {
        return groundTruthScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
