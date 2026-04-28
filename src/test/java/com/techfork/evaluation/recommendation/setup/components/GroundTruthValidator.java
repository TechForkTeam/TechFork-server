package com.techfork.evaluation.recommendation.setup.components;

import com.techfork.domain.useraccount.enums.EInterestCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GroundTruthValidator {

    /**
     * Ground Truth 품질 검증
     * - 최소 3점 이상 글이 충분한지
     * - 점수 분포가 편향되지 않았는지
     */
    public void validateGroundTruthQuality(
            Map<Long, Integer> groundTruthScores,
            List<EInterestCategory> interests) {

        if (groundTruthScores.isEmpty()) {
            log.error("Ground Truth가 비어있습니다!");
            throw new IllegalStateException("Ground Truth가 비어있습니다.");
        }

        int totalCount = groundTruthScores.size();
        long highQualityCount = groundTruthScores.values().stream()
                .filter(score -> score >= 3)
                .count();

        double highQualityRatio = (double) highQualityCount / totalCount;

        log.info("===== Ground Truth 품질 검증 =====");
        log.info("총 개수: {}", totalCount);
        log.info("3점 이상: {} 개 ({}%)", highQualityCount, String.format("%.1f", highQualityRatio * 100));

        // 경고: 3점 이상이 50% 미만
        if (highQualityRatio < 0.5) {
            log.warn("⚠️ 경고: 3점 이상 비율이 낮습니다 ({}%). 관심사와 맞는 글이 부족할 수 있습니다.",
                    String.format("%.1f", highQualityRatio * 100));
            log.warn("관심사: {}", interests);
        }

        // 에러: 3점 이상이 20% 미만
        if (highQualityRatio < 0.2) {
            log.error("❌ Ground Truth 품질이 너무 낮습니다. 관심사와 맞는 게시글이 부족합니다.");
            throw new IllegalStateException(
                    String.format("Ground Truth 품질 불량: 3점 이상 비율 %.1f%% (최소 20%% 필요)",
                            highQualityRatio * 100));
        }

        // 최고 점수 확인
        int maxScore = groundTruthScores.values().stream()
                .max(Integer::compareTo)
                .orElse(0);

        if (maxScore < 3) {
            log.warn("⚠️ 경고: 최고 점수가 {}점입니다. 관련도 높은 글이 없을 수 있습니다.", maxScore);
        }

        log.info("✓ Ground Truth 품질 검증 통과");
    }
}
