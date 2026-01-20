package com.techfork.domain.recommendation;

import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.EInterestCategory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 추천 시스템 평가를 위한 테스트 데이터 생성
 */
@Tag("evaluation-setup")
@Disabled("데이터 셋업용 - CI 제외")
@Slf4j
public class RecommendationTestDataSetup extends RecommendationTestBase {

    @Test
    @DisplayName("테스트 데이터 생성 (5명)")
    void generateTestData() {
        log.info("===== 테스트 데이터 생성 =====");

        List<List<EInterestCategory>> interestCombos = Arrays.asList(
                Arrays.asList(EInterestCategory.BACKEND),
                Arrays.asList(EInterestCategory.FRONTEND),
                Arrays.asList(EInterestCategory.AI_ML),
                Arrays.asList(EInterestCategory.BACKEND, EInterestCategory.DATABASE),
                Arrays.asList(EInterestCategory.AI_ML, EInterestCategory.DATA_SCIENCE)
        );

        for (int i = 0; i < 5; i++) {
            List<EInterestCategory> interests = interestCombos.get(i % interestCombos.size());
            User user = testDataGenerator.createTestUser(interests, 100);
            log.info("사용자 생성 완료: ID={}, 관심사={}", user.getId(), interests);
        }

        log.info("5명 사용자 생성 완료");
    }
}
