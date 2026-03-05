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
 * [Phase 5] 쿼리 구조 개선: bool should → dis_max
 *
 * <p>기존 bool should 구조에서 title/summary가 exact/fuzzy 양쪽에 매칭될 때
 * 점수가 2배 누적되는 구조적 불균형을 dis_max로 수정한다.
 *
 * <p>공통 고정값: titleBoost=0.15, k=20, c=60, fuzzyBoost=1.0, vectorChunkBoost=0.15
 *
 * <ul>
 *   <li>시나리오 1: 기존 bool 구조 챔피언 재평가 (Baseline)</li>
 *   <li>시나리오 2: dis_max + chunk 너프 (summaryBoost=0.75, bm25ChunkBoost=0.10)</li>
 *   <li>시나리오 3: dis_max + exact 맹신형 (exactBoost=3.0)</li>
 * </ul>
 */
@DisplayName("[Phase 5] 쿼리 구조 개선: bool should → dis_max")
class SearchPhase5QueryStructureTest extends SearchEvaluationTestBase {

    @Test
    @DisplayName("Phase 5: bool vs dis_max 구조 비교")
    void evaluate() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        Map<String, ScenarioMetrics> results = runEvaluation(
                "Phase 5: 쿼리 구조 개선 (bool → dis_max)", createScenarios(), groundTruths, true);
        saveReport("evaluation-report-phase5.json", "Phase 5: 쿼리 구조 개선 (bool → dis_max)",
                results, groundTruths.size());
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // 시나리오 1: 기존 챔피언 파라미터를 dis_max 구조에서 재평가 (Baseline)
        // Phase 2 최적값을 그대로 dis_max에 적용 — 구조 변경 효과의 순수 기준점
        scenarios.put("1. 기존 챔피언 재평가 (dis_max, summary=0.70, bm25Chunk=0.15, exact=2.0, tie=0.3)",
                createProperties(
                        0.15f, 0.70f, 0.15f, 0.15f,
                        2.0f, 1.0f, 0.3f,
                        20, 60));

        // 시나리오 2: dis_max + chunk 너프
        // dis_max(exact, fuzzy, tieBreaker=0.3) → max + 0.3×min으로 중복 패널티
        // chunk도 동등한 조건에서 경쟁하므로 0.15는 노이즈 가능성 → 0.10으로 너프
        scenarios.put("2. dis_max + chunk 너프 (dis_max, summary=0.75, bm25Chunk=0.10, exact=2.0, tie=0.3)",
                createProperties(
                        0.15f, 0.75f, 0.10f, 0.15f,
                        2.0f, 1.0f, 0.3f,
                        20, 60));

        // 시나리오 3: dis_max + exact 맹신형
        // 개발자 쿼리는 정확 일치가 더 중요하다는 가설 검증
        scenarios.put("3. dis_max + exact 맹신형 (dis_max, summary=0.75, bm25Chunk=0.10, exact=3.0, tie=0.3)",
                createProperties(
                        0.15f, 0.75f, 0.10f, 0.15f,
                        3.0f, 1.0f, 0.3f,
                        20, 60));

        return scenarios;
    }
}
