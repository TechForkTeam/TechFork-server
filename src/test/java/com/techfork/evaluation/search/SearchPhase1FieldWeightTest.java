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
 * [Phase 1] 필드 가중치 최적화: Title vs Summary vs Chunk
 *
 * <p>단일 필드 집중(3개) + 쌍 조합(2개) + 균등(1개) 시나리오로 어떤 필드가
 * 검색 품질에 가장 영향을 미치는지 파악한다.
 * 결과에 따라 유망한 방향으로 2차 탐색 시나리오를 구성한다.
 */
@DisplayName("[Phase 1] 필드 가중치 최적화")
class SearchPhase1FieldWeightTest extends SearchEvaluationTestBase {

    @Test
    @DisplayName("필드 가중치 최적화: Title vs Summary vs Chunk")
    void evaluate() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        Map<String, ScenarioMetrics> results = runEvaluation(
                "Phase 1: 필드 가중치 최적화", createScenarios(), groundTruths, true);
        saveReport("evaluation-report-phase1.json", "Phase 1: 필드 가중치 최적화",
                results, groundTruths.size());
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        scenarios.put("1. Title 중심",
                createProperties(0.6f, 0.2f, 0.2f, 60, 200));

        scenarios.put("2. Summary 중심",
                createProperties(0.2f, 0.6f, 0.2f, 60, 200));

        scenarios.put("3. Chunk 중심 (본문)",
                createProperties(0.2f, 0.2f, 0.6f, 60, 200));

        scenarios.put("4. Title+Summary 중심",
                createProperties(0.45f, 0.45f, 0.1f, 60, 200));

        scenarios.put("5. Summary+Chunk 중심",
                createProperties(0.1f, 0.45f, 0.45f, 60, 200));

        scenarios.put("6. 균등 가중치",
                createProperties(0.33f, 0.33f, 0.34f, 60, 200));

        scenarios.put("7. Title+Summary만 (Chunk 제외)",
                createProperties(0.5f, 0.5f, 0.0f, 60, 200));

        return scenarios;
    }
}