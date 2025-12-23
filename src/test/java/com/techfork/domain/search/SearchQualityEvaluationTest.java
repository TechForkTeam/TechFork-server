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
    @DisplayName("[Phase 1] 필드 가중치 최적화: Title vs Summary vs Content")
    void evaluatePhase1_FieldWeights() throws IOException {
        runEvaluation("Phase 1", createPhase1Scenarios(), false);
    }

    @Test
    @DisplayName("[Phase 2] 검색 방식 최적화: Keyword(BM25) vs Semantic(Vector)")
    void evaluatePhase2_KeywordVsSemantic() throws IOException {
        runEvaluation("Phase 2", createPhase2Scenarios(), false);
    }

    @Test
    @DisplayName("[Phase 3] KNN 파라미터 최적화: k/candidates - 속도 vs 품질 Trade-off")
    void evaluatePhase3_KnnParameters() throws IOException {
        Map<String, GeneralSearchProperties> scenarios = createPhase3Scenarios();

        log.info("🔥 Warm-up 라운드 시작 (캐싱 효과 제거)");
        runEvaluationWithWarmup("Phase 3", scenarios, true);
    }

    private void runEvaluationWithWarmup(String phaseName, Map<String, GeneralSearchProperties> scenarios, boolean measureLatency) throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        if (groundTruths.isEmpty()) {
            log.warn("⚠️ Ground Truth 데이터가 없습니다. src/test/resources/ground-truth.json 파일을 확인하세요.");
            return;
        }

        // 1. Warm-up 라운드: 캐시 예열 및 JIT 최적화
        log.info("🔥 Warm-up 라운드 실행 중...");
        scenarios.forEach((scenarioName, props) -> {
            SearchService warmupService = new SearchServiceImpl(
                    elasticsearchClient,
                    embeddingClient,
                    props,
                    userProfileDocumentRepository,
                    searchAsyncExecutor
            );
            // 각 쿼리를 한 번씩 실행 (결과는 버림)
            groundTruths.forEach(item -> warmupService.searchGeneral(item.getQuery()));
        });
        log.info("✅ Warm-up 완료\n");

        // 2. 실제 측정: 순서를 랜덤화하여 2번 실행 후 평균
        log.info("==================================================================================");
        log.info("🔥 {} 검색 품질 평가 시작 (쿼리 개수: {}, 시나리오 개수: {}, 반복: 2회)", phaseName, groundTruths.size(), scenarios.size());
        log.info("==================================================================================");

        Map<String, double[]> results = new LinkedHashMap<>(); // scenarioName -> [nDCG, Recall, Latency]

        // 2회 반복 실행
        for (int round = 1; round <= 2; round++) {
            log.info("\n📊 Round {}/2", round);

            // 시나리오 순서를 랜덤하게 섞음
            List<Map.Entry<String, GeneralSearchProperties>> shuffledScenarios = new ArrayList<>(scenarios.entrySet());
            Collections.shuffle(shuffledScenarios);

            for (Map.Entry<String, GeneralSearchProperties> entry : shuffledScenarios) {
                String scenarioName = entry.getKey();
                GeneralSearchProperties props = entry.getValue();

                List<Double> nDcgScores = new ArrayList<>();
                List<Double> recallScores = new ArrayList<>();
                long totalLatency = 0;

                SearchService currentSearchService = new SearchServiceImpl(
                        elasticsearchClient,
                        embeddingClient,
                        props,
                        userProfileDocumentRepository,
                        searchAsyncExecutor
                );

                for (GroundTruthItem item : groundTruths) {
                    long startTime = measureLatency ? System.currentTimeMillis() : 0;
                    List<SearchResult> searchResults = currentSearchService.searchGeneral(item.getQuery());
                    if (measureLatency) {
                        totalLatency += (System.currentTimeMillis() - startTime);
                    }

                    List<String> actualDocIds = searchResults.stream()
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
                double avgLatency = measureLatency ? (double) totalLatency / groundTruths.size() : 0;

                // 누적 평균 계산
                results.computeIfAbsent(scenarioName, k -> new double[3]);
                results.get(scenarioName)[0] += avgNDCG;
                results.get(scenarioName)[1] += avgRecall;
                results.get(scenarioName)[2] += avgLatency;
            }
        }

        // 최종 평균 출력 (원래 순서대로)
        log.info("\n==================================================================================");
        log.info("📊 최종 결과 (2회 평균)");
        log.info("==================================================================================");

        scenarios.forEach((scenarioName, props) -> {
            double[] avgResults = results.get(scenarioName);
            double finalNDCG = avgResults[0] / 2.0;
            double finalRecall = avgResults[1] / 2.0;
            double finalLatency = avgResults[2] / 2.0;

            if (measureLatency) {
                printScenarioResultWithLatency(scenarioName, props, finalNDCG, finalRecall, finalLatency);
            } else {
                printScenarioResult(scenarioName, props, finalNDCG, finalRecall);
            }
        });

        log.info("==================================================================================");
        log.info("🏁 {} 검색 품질 평가 종료", phaseName);
        log.info("==================================================================================");
    }

    private void runEvaluation(String phaseName, Map<String, GeneralSearchProperties> scenarios, boolean measureLatency) throws IOException {
        List<GroundTruthItem> groundTruths = loadGroundTruth();
        if (groundTruths.isEmpty()) {
            log.warn("⚠️ Ground Truth 데이터가 없습니다. src/test/resources/ground-truth.json 파일을 확인하세요.");
            return;
        }

        log.info("==================================================================================");
        log.info("🔥 {} 검색 품질 평가 시작 (쿼리 개수: {}, 시나리오 개수: {})", phaseName, groundTruths.size(), scenarios.size());
        log.info("==================================================================================");

        scenarios.forEach((scenarioName, props) -> {
            List<Double> nDcgScores = new ArrayList<>();
            List<Double> recallScores = new ArrayList<>();
            long totalLatency = 0;

            SearchService currentSearchService = new SearchServiceImpl(
                    elasticsearchClient,
                    embeddingClient,
                    props,
                    userProfileDocumentRepository,
                    searchAsyncExecutor
            );

            for (GroundTruthItem item : groundTruths) {
                long startTime = measureLatency ? System.currentTimeMillis() : 0;
                List<SearchResult> results = currentSearchService.searchGeneral(item.getQuery());
                if (measureLatency) {
                    totalLatency += (System.currentTimeMillis() - startTime);
                }

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

            if (measureLatency) {
                double avgLatency = (double) totalLatency / groundTruths.size();
                printScenarioResultWithLatency(scenarioName, props, avgNDCG, avgRecall, avgLatency);
            } else {
                printScenarioResult(scenarioName, props, avgNDCG, avgRecall);
            }
        });

        log.info("==================================================================================");
        log.info("🏁 {} 검색 품질 평가 종료", phaseName);
        log.info("==================================================================================");
    }

    private Map<String, GeneralSearchProperties> createPhase1Scenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // Phase 1: 필드 가중치 최적화 - Keyword/Semantic 비율은 동일하게 유지
        // 목적: Title, Summary, Content 중 어떤 필드가 검색 품질에 가장 중요한지 파악

        // 1. Baseline (균형잡힌 기본 설정)
        scenarios.put("1. Baseline (균형)",
                createProperties(2.0f, 1.5f, 1.0f, 2.0f, 1.5f, 1.0f, 30.0, 60, 200));

        // 2. Title Focus (제목 집중)
        scenarios.put("2. Title Focus (제목 집중)",
                createProperties(3.0f, 1.0f, 0.5f, 3.0f, 1.0f, 0.5f, 30.0, 60, 200));

        // 3. Summary Focus (요약 집중)
        scenarios.put("3. Summary Focus (요약 집중)",
                createProperties(1.0f, 3.0f, 0.5f, 1.0f, 3.0f, 0.5f, 30.0, 60, 200));

        // 4. Content Focus (본문 집중)
        scenarios.put("4. Content Focus (본문 집중)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 60, 200));

        // 5. Title+Summary Focus (제목+요약 집중)
        scenarios.put("5. Title+Summary Focus (제목+요약)",
                createProperties(2.5f, 2.5f, 0.5f, 2.5f, 2.5f, 0.5f, 30.0, 60, 200));

        // 6. Equal Balance (동일 가중치)
        scenarios.put("6. Equal Balance (동일 가중치)",
                createProperties(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 30.0, 60, 200));

        return scenarios;
    }

    private Map<String, GeneralSearchProperties> createPhase2Scenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // Phase 2: 검색 방식 최적화 (Keyword vs Semantic)
        // Phase 1 결과: Content Focus(본문 집중)가 최고 성능 → 필드 비율 Title:1.0, Summary:1.0, Chunk:3.0 기반
        // 목적: BM25(Keyword) vs Vector(Semantic) 중 어느 방식이 더 효과적인지 파악

        // 1. Baseline (Keyword/Semantic 균형) - Phase 1 최적 설정
        scenarios.put("1. Baseline (Keyword=Semantic 균형)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 60, 200));

        // 2. Keyword Moderate (BM25 중간 강화)
        scenarios.put("2. Keyword Moderate (BM25 강화)",
                createProperties(1.5f, 1.5f, 4.0f, 0.8f, 0.8f, 2.5f, 30.0, 60, 200));

        // 3. Keyword Heavy (BM25 극대화)
        scenarios.put("3. Keyword Heavy (BM25 극대화)",
                createProperties(2.0f, 2.0f, 5.0f, 0.5f, 0.5f, 2.0f, 30.0, 60, 200));

        // 4. Semantic Moderate (Vector 중간 강화)
        scenarios.put("4. Semantic Moderate (Vector 강화)",
                createProperties(0.8f, 0.8f, 2.5f, 1.5f, 1.5f, 4.0f, 30.0, 60, 200));

        // 5. Semantic Heavy (Vector 극대화)
        scenarios.put("5. Semantic Heavy (Vector 극대화)",
                createProperties(0.5f, 0.5f, 2.0f, 2.0f, 2.0f, 5.0f, 30.0, 60, 200));

        // 6. Keyword Only (BM25만 사용)
        scenarios.put("6. Keyword Only (BM25 전용)",
                createProperties(1.0f, 1.0f, 3.0f, 0.1f, 0.1f, 0.5f, 30.0, 60, 200));

        // 7. Semantic Only (Vector만 사용)
        scenarios.put("7. Semantic Only (Vector 전용)",
                createProperties(0.1f, 0.1f, 0.5f, 1.0f, 1.0f, 3.0f, 30.0, 60, 200));

        return scenarios;
    }

    private Map<String, GeneralSearchProperties> createPhase3Scenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // Phase 3: KNN 파라미터 최적화 (k, candidates)
        // Phase 1 최적 필드 가중치: Title:1.0, Summary:1.0, Chunk:3.0
        // Phase 2 최적 검색 방식: Baseline (Keyword=Semantic 균형)
        // 목적: k/candidates 값에 따른 속도 vs 품질 trade-off 파악

        // 1. Baseline (Phase 2 최적 설정)
        scenarios.put("1. Baseline (k=60, c=200)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 60, 200));

        // 2. Fast (속도 최우선)
        scenarios.put("2. Fast (k=20, c=60)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 20, 60));

        // 3. Small (속도 우선)
        scenarios.put("3. Small (k=30, c=90)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 30, 90));

        // 4. Balanced (균형)
        scenarios.put("4. Balanced (k=40, c=120)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 40, 120));

        // 5. Medium (중간)
        scenarios.put("5. Medium (k=50, c=150)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 50, 150));

        // 6. Large (품질 우선)
        scenarios.put("6. Large (k=80, c=250)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 80, 250));

        // 7. XLarge (품질 최우선)
        scenarios.put("7. XLarge (k=100, c=300)",
                createProperties(1.0f, 1.0f, 3.0f, 1.0f, 1.0f, 3.0f, 30.0, 100, 300));

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

    private void printScenarioResult(String name, GeneralSearchProperties p, double nDCG, double recall) {
        System.out.println("\n#######################################################################################");
        System.out.println("▶ " + name);
        System.out.printf("   [Lexical] Title:%.1f | Summary:%.1f | Chunk:%.1f\n", p.getTitleBoost(), p.getSummaryBoost(), p.getChunkBoost());
        System.out.printf("   [Vector ] Title:%.1f | Summary:%.1f | Chunk:%.1f\n", p.getVectorTitleBoost(), p.getVectorSummaryBoost(), p.getVectorContentChunkBoost());
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.printf("   📊 nDCG@%d   : %.4f  (랭킹 품질)\n", nDCG_K, nDCG);
        System.out.printf("   📊 Recall@%d : %.4f  (재현율)\n", RECALL_K, recall);
        System.out.println("#######################################################################################");
    }

    private void printScenarioResultWithLatency(String name, GeneralSearchProperties p, double nDCG, double recall, double latency) {
        System.out.println("\n#######################################################################################");
        System.out.println("▶ " + name);
        System.out.printf("   [KNN] k=%d | candidates=%d | ratio=%.2f\n", p.getKnnK(), p.getKnnNumCandidates(), (double) p.getKnnNumCandidates() / p.getKnnK());
        System.out.printf("   [Boost] Lexical(%.1f/%.1f/%.1f) | Vector(%.1f/%.1f/%.1f)\n",
                p.getTitleBoost(), p.getSummaryBoost(), p.getChunkBoost(),
                p.getVectorTitleBoost(), p.getVectorSummaryBoost(), p.getVectorContentChunkBoost());
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.printf("   📊 nDCG@%d   : %.4f  (랭킹 품질)\n", nDCG_K, nDCG);
        System.out.printf("   📊 Recall@%d : %.4f  (재현율)\n", RECALL_K, recall);
        System.out.printf("   ⏱️ Latency   : %.2f ms (평균 응답시간)\n", latency);
        System.out.println("#######################################################################################");
    }
}