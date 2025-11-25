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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;

@Slf4j
@SpringBootTest
class SearchQualityEvaluationTest {

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
    @Qualifier("searchAsyncExecutor")
    private Executor searchAsyncExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int nDCG_K = 10;
    private static final int RECALL_K = 5;

    @Test
    @DisplayName("다양한 검색 하이퍼파라미터 시나리오별 품질(nDCG, Recall) 비교 평가")
    void evaluateSearchQualityAcrossScenarios() throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        if (groundTruths.isEmpty()) {
            log.warn("⚠️ Ground Truth 데이터가 없습니다. src/test/resources/ground-truth.json 파일을 확인하세요.");
            return;
        }

        Map<String, GeneralSearchProperties> scenarios = createScenarios();

        log.info("==================================================================================");
        log.info("🔥 검색 품질 비교 평가 시작 (쿼리 개수: {}, 시나리오 개수: {})", groundTruths.size(), scenarios.size());
        log.info("==================================================================================");

        scenarios.forEach((scenarioName, props) -> {
            List<Double> nDcgScores = new ArrayList<>();
            List<Double> recallScores = new ArrayList<>();

            SearchService currentSearchService = new SearchServiceImpl(
                    elasticsearchClient,
                    embeddingClient,
                    props,
                    userProfileDocumentRepository,
                    userRepository,
                    searchAsyncExecutor
            );

            long totalLatency = 0;

            for (GroundTruthItem item : groundTruths) {
                long startTime = System.currentTimeMillis();
                List<SearchResult> results = currentSearchService.searchGeneral(item.getQuery());
                totalLatency += (System.currentTimeMillis() - startTime);

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

            double avgNDCG = nDcgScores.stream().mapToDouble(d -> d).average().orElse(0.0);
            double avgRecall = recallScores.stream().mapToDouble(d -> d).average().orElse(0.0);
            double avgLatency = (double) totalLatency / groundTruths.size();

            printScenarioResult(scenarioName, props, avgNDCG, avgRecall, avgLatency);
        });

        log.info("==================================================================================");
        log.info("🏁 검색 품질 비교 평가 종료");
        log.info("==================================================================================");
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // 1. Baseline (기본값 - 현재 설정)
        scenarios.put("1. Baseline (현재 설정)",
                createProperties(5.0f, 1.0f, 0.5f, 3.0f, 1.5f, 0.8f, 30.0, 60, 200));

        // 2. Keyword Intensive (키워드 가중치 극대화)
        scenarios.put("2. Keyword Intensive (제목/본문 매칭 강화)",
                createProperties(10.0f, 3.0f, 1.0f, 1.0f, 0.5f, 0.5f, 10.0, 60, 100));

        // 3. Semantic Intensive (의미 기반 검색 강화)
        scenarios.put("3. Semantic Intensive (벡터 가중치 강화)",
                createProperties(1.0f, 1.0f, 0.5f, 10.0f, 5.0f, 3.0f, 50.0, 100, 300));

        // 4. Title Focus (제목 몰빵)
        scenarios.put("4. Title Focus (제목 키워드 & 벡터 최우선)",
                createProperties(10.0f, 0.5f, 0.1f, 10.0f, 1.0f, 0.5f, 30.0, 60, 200));

        // 5. Balanced High Recall (넓게 찾기)
        scenarios.put("5. High Recall (탐색 범위 확장)",
                createProperties(3.0f, 2.0f, 1.0f, 3.0f, 2.0f, 1.0f, 20.0, 150, 400));

        return scenarios;
    }

    private GeneralSearchProperties createProperties(
            float titleBoost, float summaryBoost, float chunkBoost,
            float vecTitleBoost, float vecSummaryBoost, float vecChunkBoost,
            double hybridWeight, int k, int candidates) {

        GeneralSearchProperties props = new GeneralSearchProperties();

        props.setSearchSize(15);

        // Lexical(BM25) 가중치
        props.setTitleBoost(titleBoost);
        props.setSummaryBoost(summaryBoost);
        props.setChunkBoost(chunkBoost);

        // Semantic(Vector) 가중치
        props.setVectorTitleBoost(vecTitleBoost);
        props.setVectorSummaryBoost(vecSummaryBoost);
        props.setVectorContentChunkBoost(vecChunkBoost);

        // RRF 및 KNN 설정
        props.setKnnK(k);
        props.setKnnNumCandidates(candidates);

        // 리랭킹 가중치
        props.setHybridScoreWeight(hybridWeight);
        props.setPersonalScoreWeight(1.0);

        // RRF 상수
        props.setRRF_K(60);
        props.setRRF_WINDOW_SIZE(60);

        return props;
    }

    private List<GroundTruthItem> loadGroundTruth() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("ground-truth.json");
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
        } catch (IOException e) {
            log.error("❌ ground-truth.json 로드 실패: src/test/resources 경로를 확인하세요.", e);
            throw e;
        }
    }

    private void printScenarioResult(String name, GeneralSearchProperties p, double nDCG, double recall, double latency) {
        System.out.println("\n#######################################################################################");
        System.out.println("▶ " + name);
        System.out.printf("   [Lexical] Title:%.1f | Summary:%.1f | Chunk:%.1f\n", p.getTitleBoost(), p.getSummaryBoost(), p.getChunkBoost());
        System.out.printf("   [Vector ] Title:%.1f | Summary:%.1f | Chunk:%.1f | K:%d\n", p.getVectorTitleBoost(), p.getVectorSummaryBoost(), p.getVectorContentChunkBoost(), p.getKnnK());
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.printf("   📊 nDCG@%d   : %.4f  (정확도)\n", nDCG_K, nDCG);
        System.out.printf("   📊 Recall@%d : %.4f  (재현율)\n", RECALL_K, recall);
        System.out.printf("   ⏱️ Latency   : %.2f ms\n", latency);
        System.out.println("#######################################################################################");
    }
}