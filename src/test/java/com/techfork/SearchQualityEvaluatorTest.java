package com.techfork;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.domain.search.GroundTruthItem;
import com.techfork.domain.search.SearchQualityService;
import com.techfork.domain.search.SearchResult;
import com.techfork.domain.search.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
public class SearchQualityEvaluatorTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private SearchQualityService searchQualityService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1단계 일반 검색 품질 평가 (nDCG@10, Recall@5)")
    void evaluateSearchQuality() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();

        List<Double> nDcgScores = new ArrayList<>();
        List<Double> recallScores = new ArrayList<>();

        log.info("===== 검색 품질 평가 시작 (총 {}개 시나리오) =====", groundTruths.size());

        for (GroundTruthItem item : groundTruths) {
            String query = item.getQuery();
            Map<String, Integer> idealMap = item.getIdealResultsMap();

            List<SearchResult> results = searchService.searchGeneral(query);

            List<String> actualDocIds = results.stream()
                    .map(result -> String.valueOf(result.getPostId()))
                    .toList();

            double nDCG = searchQualityService.calculateNDCG(actualDocIds, idealMap, 10);
            double recall = searchQualityService.calculateRecall(actualDocIds, idealMap.keySet(), 5);

            nDcgScores.add(nDCG);
            recallScores.add(recall);

            log.info("[Query: {}] nDCG@10: {}, Recall@5: {}",
                    query, String.format("%.4f", nDCG), String.format("%.4f", recall));
        }

        double avgNDCG = nDcgScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgRecall = recallScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        log.info("===== 최종 평가 결과 =====");
        log.info("Average nDCG@10: {}", String.format("%.4f", avgNDCG));
        log.info("Average Recall@5: {}", String.format("%.4f", avgRecall));
    }

    private List<GroundTruthItem> loadGroundTruth() throws IOException {
        // src/test/resources/ground-truth.json 파일을 읽음
        ClassPathResource resource = new ClassPathResource("ground-truth.json");
        return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
    }
}