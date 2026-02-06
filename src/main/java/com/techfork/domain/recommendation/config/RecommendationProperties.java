package com.techfork.domain.recommendation.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {

    private Integer knnSearchSize = 80;

    private Integer numCandidates = 180;

    private Integer mmrCandidateSize = 80;

    private Integer mmrFinalSize = 30;

    private Double lambda = 0.95;

    private Integer activeUserHours = 24;

    private EmbeddingWeights embeddingWeights = new EmbeddingWeights();

    private TimeDecay timeDecay = new TimeDecay();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingWeights {
        private Float title = 0.4f;
        private Float summary = 0.4f;
        private Float content = 0.2f;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeDecay {
        private Double days7 = 1.3;
        private Double days30 = 1.0;
        private Double days90 = 0.7;
        private Double daysOver = 0.4;
    }
}
