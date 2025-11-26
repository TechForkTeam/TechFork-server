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

        // 6. Content Deep Dive (본문 내용 집중)
        // 목적: 제목이나 요약에는 없지만, 본문 구석에 있는 구체적인 해결책이나 에러 로그 등을 찾을 때 유리한지 확인
        scenarios.put("5. Content Deep Dive (본문 청크 집중)",
                createProperties(1.0f, 1.0f, 5.0f, 1.0f, 1.0f, 4.0f, 30.0, 60, 200));

        // 7. Summary Oriented (요약문 집중)
        // 목적: 제목이 너무 함축적(예: "나의 회고")일 때, 요약문에 포함된 핵심 의도를 잘 파악하는지 확인
        scenarios.put("6. Summary Oriented (요약문 집중)",
                createProperties(1.0f, 4.0f, 1.0f, 1.0f, 4.0f, 1.0f, 30.0, 60, 200));

        // 8. High Precision / Low Latency (속도 최적화 & 상위 매칭)
        // 목적: 검색 후보군(Candidates)을 줄였을 때, 정확도(nDCG) 손실은 적으면서 응답 속도(Latency)가 얼마나 개선되는지 확인
        scenarios.put("7. High Precision (속도 중시, 후보군 축소)",
                createProperties(3.0f, 1.0f, 0.5f, 3.0f, 1.5f, 0.8f, 30.0, 20, 40));

        // 10. Equal Balance (모든 필드 동등 가중치)
        // 목적: 특정 필드에 가중치를 주지 않고 기계적으로 동등하게 설정했을 때의 베이스라인 품질 확인
        scenarios.put("8. Equal Balance (모든 가중치 동일)",
                createProperties(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 30.0, 60, 200));

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