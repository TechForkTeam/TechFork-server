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
 * [Phase 2] 필드 가중치 세부 조정: Phase 1 결과 기반 미세 탐색
 *
 * <p>Phase 1에서 유망한 방향이 확인된 후 시나리오를 채워 넣는다.
 * 시나리오가 비어 있으면 조기 종료해 실수로 실행해도 안전하다.
 */
@DisplayName("[Phase 2] 필드 가중치 세부 조정")
class SearchPhase2FieldWeightDetailTest extends SearchEvaluationTestBase {

    @Test
    @DisplayName("필드 가중치 세부 조정: Phase 1 결과 기반 미세 탐색")
    void evaluate() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        Map<String, ScenarioMetrics> results = runEvaluation(
                "Phase 2: 필드 가중치 세부 조정", createScenarios(), groundTruths, false);
        saveReport("evaluation-report-phase2.json", "Phase 2: 필드 가중치 세부 조정",
                results, groundTruths.size());
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // Phase 1 최고 시나리오 (Baseline)
        scenarios.put("1. Phase1 Summary 중심 (Baseline, 0.20/0.60/0.20)",
                createProperties(0.2f, 0.6f, 0.2f, 60, 200));

        // Summary 비율 올리기
        scenarios.put("2. Summary 강화 (0.15/0.70/0.15)",
                createProperties(0.15f, 0.70f, 0.15f, 60, 200));

        scenarios.put("3. Summary 최대 (0.10/0.80/0.10)",
                createProperties(0.10f, 0.80f, 0.10f, 60, 200));

        // Summary 고정(0.60), Title vs Chunk 비율 조정
        scenarios.put("4. Chunk 비중 확대 (0.10/0.60/0.30)",
                createProperties(0.10f, 0.60f, 0.30f, 60, 200));

        scenarios.put("5. Title 비중 확대 (0.30/0.60/0.10)",
                createProperties(0.30f, 0.60f, 0.10f, 60, 200));

        // Summary 소폭 낮추고 Title 보강
        scenarios.put("6. Summary+Title 균형 (0.30/0.55/0.15)",
                createProperties(0.30f, 0.55f, 0.15f, 60, 200));

        scenarios.put("7. Summary+Title 균형 강화 (0.35/0.55/0.10)",
                createProperties(0.35f, 0.55f, 0.10f, 60, 200));

        return scenarios;
    }
}
