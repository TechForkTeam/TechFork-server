package com.techfork.domain.search_quality;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class GroundTruthItem {
    private String query;
    private List<IdealResult> idealResults;

    @Data
    @NoArgsConstructor
    public static class IdealResult {
        private String docId;
        private int relevance;  // 관련도 점수 (3: 매우 관련, 2: 관련, 1: 약간 관련)
    }

    public Map<String, Integer> getIdealResultsMap() {
        return idealResults.stream()
                .collect(Collectors.toMap(IdealResult::getDocId, IdealResult::getRelevance));
    }
}