package com.techfork.evaluation.search;

import com.techfork.domain.search.config.GeneralSearchProperties;
import com.techfork.evaluation.search.util.GroundTruthItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [Phase 4] KNN 파라미터 최적화: k/candidates - 속도 vs 품질 Trade-off
 *
 * <p>Phase 1 최적 필드 가중치를 고정한 상태에서 k/candidates 조합별
 * 품질(nDCG/Recall)과 응답 latency를 측정한다.
 */
@DisplayName("[Phase 4] KNN 파라미터 최적화")
class SearchPhase4KnnParameterTest extends SearchEvaluationTestBase {

    @Test
    @DisplayName("KNN 파라미터 최적화: k/candidates - 속도 vs 품질 Trade-off")
    void evaluate() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        Map<String, ScenarioMetrics> results = runEvaluation(
                "Phase 4: KNN 파라미터 최적화", createScenarios(), groundTruths, true);
        saveReport("evaluation-report-phase4.json", "Phase 4: KNN 파라미터 최적화",
                results, groundTruths.size());
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // Phase 2 최적값 적용: title=0.15, summary=0.70, chunk=0.15 / Phase 3: chunk 모두 포함
        scenarios.put("1. k=20, c=60",
                createProperties(0.15f, 0.70f, 0.15f, 20, 60));

        scenarios.put("2. k=30, c=90",
                createProperties(0.15f, 0.70f, 0.15f, 30, 90));

        scenarios.put("3. k=40, c=120",
                createProperties(0.15f, 0.70f, 0.15f, 40, 120));

        scenarios.put("4. k=50, c=150",
                createProperties(0.15f, 0.70f, 0.15f, 50, 150));

        scenarios.put("5. k=60, c=200",
                createProperties(0.15f, 0.70f, 0.15f, 60, 200));

        scenarios.put("6. k=80, c=250",
                createProperties(0.15f, 0.70f, 0.15f, 80, 250));

        scenarios.put("7. k=100, c=300",
                createProperties(0.15f, 0.70f, 0.15f, 100, 300));

        return scenarios;
    }
}
