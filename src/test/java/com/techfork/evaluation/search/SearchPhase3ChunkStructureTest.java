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
 * [Phase 3] Chunk 구조 최적화: BM25/Vector chunk 포함/제외 실험
 *
 * <p>BM25 chunk boost와 vector chunk boost를 독립적으로 제어해
 * 각 경로에서 chunk의 기여도를 측정한다.
 * - title/summary 가중치: Phase 2 최적값으로 수정 필요
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

        // Phase 2 최적값 적용: title=0.15, summary=0.70, chunk(BM25)=0.15, chunk(Vector)=0.15
        scenarios.put("1. Chunk 모두 포함 (Baseline)",
                createProperties(0.15f, 0.70f, 0.15f, 0.15f, 60, 200));

        scenarios.put("2. BM25 Chunk 제외",
                createProperties(0.15f, 0.70f, 0.0f, 0.15f, 60, 200));

        scenarios.put("3. Vector Chunk 제외",
                createProperties(0.15f, 0.70f, 0.15f, 0.0f, 60, 200));

        return scenarios;
    }
}
