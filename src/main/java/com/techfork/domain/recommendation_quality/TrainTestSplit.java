package com.techfork.domain.recommendation_quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자 읽기 이력의 Train/Test 분할 결과
 * Train: 사용자 프로필 생성에 사용
 * Test: 평가 Ground Truth로 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainTestSplit {

    /**
     * Train 세트: 사용자 프로필 생성에 사용될 게시글 ID 목록 (80%)
     */
    private List<Long> trainPostIds;

    /**
     * Test 세트: 평가 Ground Truth로 사용될 게시글 ID 목록 (20%)
     * 추천 시스템이 이 글들을 상위권에 추천했는지 평가
     */
    private List<Long> testPostIds;

    /**
     * Train 세트 크기
     */
    public int getTrainSize() {
        return trainPostIds.size();
    }

    /**
     * Test 세트 크기
     */
    public int getTestSize() {
        return testPostIds.size();
    }
}
