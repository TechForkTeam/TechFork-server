package com.techfork.evaluation.search;

import com.techfork.domain.search.service.GeneralSearchProperties;
import com.techfork.evaluation.search.util.GroundTruthItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [Phase 2] KNN 파라미터 최적화: k/candidates - 속도 vs 품질 Trade-off
 *
 * <p>Phase 1 최적 필드 가중치를 고정한 상태에서 k/candidates 조합별
 * 품질(nDCG/Recall)과 응답 latency를 측정한다.
 */
@DisplayName("[Phase 2] KNN 파라미터 최적화")
class SearchPhase2KnnParameterTest extends SearchEvaluationTestBase {

    @Test
    @DisplayName("KNN 파라미터 최적화: k/candidates - 속도 vs 품질 Trade-off")
    void evaluate() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        Map<String, ScenarioMetrics> results = runEvaluation(
                "Phase 2: KNN 파라미터 최적화", createScenarios(), groundTruths, true);
        saveReport("evaluation-report-phase2.json", "Phase 2: KNN 파라미터 최적화",
                results, groundTruths.size());
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        scenarios.put("1. Baseline (k=60, c=200)",
                createProperties(3.0f, 1.5f, 1.0f, 60, 200));

        scenarios.put("2. Fast (k=20, c=60)",
                createProperties(3.0f, 1.5f, 1.0f, 20, 60));

        scenarios.put("3. Small (k=30, c=90)",
                createProperties(3.0f, 1.5f, 1.0f, 30, 90));

        scenarios.put("4. Balanced (k=40, c=120)",
                createProperties(3.0f, 1.5f, 1.0f, 40, 120));

        scenarios.put("5. Medium (k=50, c=150)",
                createProperties(3.0f, 1.5f, 1.0f, 50, 150));

        scenarios.put("6. Large (k=80, c=250)",
                createProperties(3.0f, 1.5f, 1.0f, 80, 250));

        scenarios.put("7. XLarge (k=100, c=300)",
                createProperties(3.0f, 1.5f, 1.0f, 100, 300));

        return scenarios;
    }
}