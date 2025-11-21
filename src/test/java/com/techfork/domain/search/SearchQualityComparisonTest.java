package com.techfork.domain.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.search.service.GeneralSearchProperties;
import com.techfork.domain.search.service.SearchService;
import com.techfork.domain.search.service.SearchServiceImpl;
import com.techfork.domain.search_quality.GroundTruthItem;
import com.techfork.domain.search_quality.SearchQualityService;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.llm.EmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@SpringBootTest
class SearchQualityComparisonTest {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private SearchQualityService searchQualityService;

    @Autowired
    private UserProfileDocumentRepository userProfileDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int nDCG_K = 10;
    private static final int RECALL_K = 5;

    @Test
    @DisplayName("다양한 하이퍼파라미터 시나리오별 검색 엔진 성능 (nDCG, Recall) 비교 평가")
    void compareSearchQualityAcrossScenarios() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        if (groundTruths.isEmpty()) {
            log.warn("Ground truth data (ground-truth.json) not found or empty. Skipping evaluation.");
            return;
        }

        Map<String, GeneralSearchProperties> scenarios = createScenarios();

        log.info("===== 검색 품질 비교 평가 시작 (총 {}개 시나리오, {}개 쿼리) =====",
                scenarios.size(), groundTruths.size());

        scenarios.forEach((scenarioName, props) -> {
            List<Double> nDcgScores = new ArrayList<>();
            List<Double> recallScores = new ArrayList<>();

            SearchService currentSearchService = new SearchServiceImpl(
                    elasticsearchClient, embeddingClient, props, userProfileDocumentRepository, userRepository
            );

            for (GroundTruthItem item : groundTruths) {
                List<SearchResult> results = currentSearchService.searchGeneral(item.getQuery());

                List<String> actualDocIds = results.stream()
                        .map(result -> String.valueOf(result.getPostId()))
                        .toList();

                Map<String, Integer> idealMap = item.getIdealResultsMap();
                Set<String> idealDocIds = idealMap.keySet();

                double nDCG = searchQualityService.calculateNDCG(actualDocIds, idealMap, nDCG_K);
                double recall = searchQualityService.calculateRecall(actualDocIds, idealDocIds, RECALL_K);

                nDcgScores.add(nDCG);
                recallScores.add(recall);
            }

            double avgNDCG = nDcgScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double avgRecall = recallScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            System.out.println("\n##########################################################");
            System.out.println("▶ 시나리오: " + scenarioName);
            System.out.printf("   [설정] Title:%.1f | Summary:%.1f | Chunk:%.1f | Semantic:%.1f\n",
                    props.getTitleBoost(), props.getSummaryBoost(), props.getChunkBoost(), props.getSemanticBoost());
            System.out.println("----------------------------------------------------------");
            System.out.printf("   ✅ Average nDCG@%d: %.4f\n", nDCG_K, avgNDCG);
            System.out.printf("   ✅ Average Recall@%d: %.4f\n", RECALL_K, avgRecall);
            System.out.println("##########################################################");
        });

        log.info("===== 검색 품질 비교 평가 종료 =====");
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // (Signature: Title, Summary, Chunk, Semantic, KNN K, Candidates)
        // -------------------------------------------------------------------------------------------------------

        // 1. Baseline (기본값)
        scenarios.put("1. Baseline (균형 잡힌 기본값)",
                createProperties(2.0f, 2.0f, 1.0f, 5.0f, 15, 30));

        // 2. Title Heavy (제목 집중)
        scenarios.put("2. Title Heavy (제목 집중)",
                createProperties(10.0f, 1.0f, 1.0f, 1.0f, 15, 30));

        // 3. Summary Heavy (요약 집중)
        scenarios.put("3. Summary Heavy (요약 집중)",
                createProperties(1.0f, 10.0f, 1.0f, 1.0f, 15, 30));

        // 4. Content Heavy (본문 Chunk 집중)
        scenarios.put("4. Content Heavy (본문 Chunk 집중)",
                createProperties(1.0f, 1.0f, 5.0f, 1.0f, 15, 30));

        // 5. Semantic Heavy (벡터 유사도 집중)
        scenarios.put("5. Semantic Heavy (벡터 유사도 집중)",
                createProperties(1.0f, 1.0f, 1.0f, 20.0f, 50, 100));

        // 6. Keyword Only (Vector 무시)
        scenarios.put("6. Keyword Only (Vector 무시)",
                createProperties(5.0f, 5.0f, 5.0f, 0.0f, 15, 30));

        // 7. Title + Summary Focused (Precision)
        scenarios.put("7. Title+Summary Focused (Precision)",
                createProperties(5.0f, 5.0f, 0.1f, 5.0f, 15, 30));

        // 8. Title + Semantic (제목+의미)
        scenarios.put("8. Title + Semantic (제목+의미)",
                createProperties(8.0f, 1.0f, 1.0f, 10.0f, 20, 40));

        // 9. High Recall (탐색 범위 확장)
        scenarios.put("9. High Recall (탐색 범위 확장)",
                createProperties(2.0f, 2.0f, 1.0f, 5.0f, 100, 200));

        // 10. Content & Summary Priority
        scenarios.put("10. Content & Summary Priority",
                createProperties(0.5f, 5.0f, 5.0f, 5.0f, 20, 40));

        return scenarios;
    }

    private GeneralSearchProperties createProperties(float titleBoost, float summaryBoost, float chunkBoost, float semanticBoost, int k, int candidates) {
        GeneralSearchProperties props = new GeneralSearchProperties();
        props.setSearchSize(10);
        props.setTitleBoost(titleBoost);
        props.setSummaryBoost(summaryBoost);
        props.setChunkBoost(chunkBoost);
        props.setSemanticBoost(semanticBoost);
        props.setKnnK(k);
        props.setKnnNumCandidates(candidates);
        return props;
    }

    /**
     * Ground Truth 데이터를 src/test/resources/ground-truth.json 파일에서 로드합니다.
     */
    private List<GroundTruthItem> loadGroundTruth() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("ground-truth.json");
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Failed to load ground-truth.json. Please ensure the file exists in src/test/resources.", e);
            throw e;
        }
    }
}