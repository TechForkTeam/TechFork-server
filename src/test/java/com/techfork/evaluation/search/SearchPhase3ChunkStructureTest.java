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
 * [Phase 3] Chunk 구조 최적화: chunk 포함/제외 실험
 *
 * <p>Phase 2 최적값(title=0.15, summary=0.70, chunk=0.15) 기준으로
 * chunk를 BM25/Vector 각각 제외했을 때의 품질 변화를 측정한다.
 * - k/candidates: 60/200 고정
 */
@DisplayName("[Phase 3] Chunk 구조 최적화")
class SearchPhase3ChunkStructureTest extends SearchEvaluationTestBase {

    @Test
    @DisplayName("Chunk 구조 최적화: BM25/Vector chunk 포함/제외 실험")
    void evaluate() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        Map<String, ScenarioMetrics> results = runEvaluation(
                "Phase 3: Chunk 구조 최적화", createScenarios(), groundTruths, true);
        saveReport("evaluation-report-phase3.json", "Phase 3: Chunk 구조 최적화",
                results, groundTruths.size());
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // Phase 3 결과: chunk 모두 포함(0.15)이 최적 → Phase 4로 이어짐
        // BM25/Vector chunk boost가 통합되어 독립 제어 불가 — 시나리오 비활성화
        // scenarios.put("2. BM25 Chunk 제외", createProperties(0.15f, 0.70f, 0.0f, 60, 200));
        // scenarios.put("3. Vector Chunk 제외", createProperties(0.15f, 0.70f, 0.0f, 60, 200));

        scenarios.put("1. Chunk 모두 포함 (최종 선택)",
                createProperties(0.15f, 0.70f, 0.15f, 60, 200));

        return scenarios;
    }
}
