package com.techfork.global.util;

import com.techfork.domain.recommendation.config.RecommendationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 선형 보간 시간 감쇠 전략
 * 구간별로 선형적으로 감소하여 부드러운 전환 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinearTimeDecayStrategy implements TimeDecayStrategy {

    private final RecommendationProperties properties;

    @Override
    public double calculateWeight(LocalDateTime publishedAt) {
        if (publishedAt == null) {
            log.warn("게시글 발행일이 null입니다. 기본 가중치 1.0 반환");
            return 1.0;
        }

        long daysOld = ChronoUnit.DAYS.between(publishedAt, LocalDateTime.now());
        RecommendationProperties.TimeDecay decay = properties.getTimeDecay();

        // 각 구간에서 선형 보간
        if (daysOld <= 7) {
            return decay.getDays7();
        } else if (daysOld <= 30) {
            // 7일 ~ 30일: days7에서 days30으로 선형 감소
            double ratio = (daysOld - 7.0) / (30.0 - 7.0);
            return decay.getDays7() * (1 - ratio) + decay.getDays30() * ratio;
        } else if (daysOld <= 90) {
            // 30일 ~ 90일: days30에서 days90으로 선형 감소
            double ratio = (daysOld - 30.0) / (90.0 - 30.0);
            return decay.getDays30() * (1 - ratio) + decay.getDays90() * ratio;
        } else {
            // 90일 이상: days90에서 daysOver로 선형 감소
            double ratio = Math.min((daysOld - 90.0) / 90.0, 1.0);  // 최대 180일까지
            return decay.getDays90() * (1 - ratio) + decay.getDaysOver() * ratio;
        }
    }
}
