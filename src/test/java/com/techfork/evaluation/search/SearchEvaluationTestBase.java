package com.techfork.evaluation.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.search.config.GeneralSearchProperties;
import com.techfork.domain.search.service.SearchService;
import com.techfork.domain.search.service.SearchServiceImpl;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocumentRepository;
import com.techfork.evaluation.search.util.GroundTruthItem;
import com.techfork.evaluation.search.util.SearchQualityService;
import com.techfork.global.config.CloudflareThirdPartyThumbnailOptimizationProperties;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * 검색 품질 평가 테스트 공통 베이스 클래스
 *
 * <p>generated-search-ground-truth.json 기반으로 nDCG/Recall@{4,8,20} 및 Latency를 측정하고
 * JSON 리포트로 저장한다.
 */
@Tag("evaluation")
@ActiveProfiles("local-tunnel")
@Slf4j
@SpringBootTest
public abstract class SearchEvaluationTestBase {

    protected static final int[] K_VALUES = {4, 8, 20};

    @Autowired protected ElasticsearchClient elasticsearchClient;
    @Autowired protected EmbeddingClient embeddingClient;
    @Autowired protected PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;
    @Autowired protected PostRepository postRepository;
    @Autowired protected BookmarkRepository bookmarkRepository;
    @Autowired @Qualifier("searchAsyncExecutor") protected Executor searchAsyncExecutor;
    @Autowired protected ObjectMapper objectMapper;

    protected final SearchQualityService searchQualityService = new SearchQualityService();

    // -----------------------------------------------------------------------
    // 공통 레코드
    // -----------------------------------------------------------------------

    protected record ScenarioMetrics(
            double ndcg4, double ndcg8, double ndcg20,
            double recall4, double recall8, double recall20,
            double avgLatencyMs
    ) {}

    // -----------------------------------------------------------------------
    // 공통 메서드
    // -----------------------------------------------------------------------

    protected List<GroundTruthItem> loadGroundTruth() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "fixtures/evaluation/generated-search-ground-truth.json");
        return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
    }

    /**
     * 시나리오별 평가 실행
     *
     * <p>ElasticsearchCacheManager가 앱 시작 시 HNSW 그래프를 메모리에 로드하므로
     * 별도 warmup 및 반복 실행 없이 단일 패스로 측정한다.
     */
    protected Map<String, ScenarioMetrics> runEvaluation(
            String phaseName,
            Map<String, GeneralSearchProperties> scenarios,
            List<GroundTruthItem> groundTruths,
            boolean measureLatency) {

        if (groundTruths.isEmpty()) {
            log.warn("⚠️ Ground Truth 데이터가 없습니다. generated-search-ground-truth.json 파일을 확인하세요.");
            return Collections.emptyMap();
        }

        log.info("==================================================================================");
        log.info("🔥 {} 시작 (쿼리: {}, 시나리오: {})",
                phaseName, groundTruths.size(), scenarios.size());
        log.info("==================================================================================");

        Map<String, ScenarioMetrics> metricsMap = new LinkedHashMap<>();

        for (Map.Entry<String, GeneralSearchProperties> entry : scenarios.entrySet()) {
            String scenarioName = entry.getKey();
            GeneralSearchProperties props = entry.getValue();
            CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer = new CloudflareThirdPartyThumbnailOptimizer(
                    new CloudflareThirdPartyThumbnailOptimizationProperties()
            );

            SearchService svc = new SearchServiceImpl(
                    elasticsearchClient, embeddingClient, props,
                    personalizationProfileDocumentRepository, postRepository,
                    bookmarkRepository, searchAsyncExecutor, thumbnailOptimizer);

            // index: [nDCG@4, nDCG@8, nDCG@20, Recall@4, Recall@8, Recall@20, latency]
            double[] sums = new double[7];
            for (GroundTruthItem item : groundTruths) {
                long start = measureLatency ? System.currentTimeMillis() : 0;
                List<SearchResult> results = svc.searchGeneral(item.getQuery());
                if (measureLatency) {
                    sums[6] += (System.currentTimeMillis() - start);
                }

                List<String> actualIds = results.stream()
                        .map(r -> String.valueOf(r.getPostId()))
                        .toList();
                Map<String, Integer> idealMap = item.getIdealResultsMap();
                Set<String> idealIds = idealMap.keySet();

                for (int i = 0; i < K_VALUES.length; i++) {
                    sums[i] += searchQualityService.calculateNDCG(actualIds, idealMap, K_VALUES[i]);
                    sums[i + 3] += searchQualityService.calculateRecall(actualIds, idealIds, K_VALUES[i]);
                }
            }

            double n = groundTruths.size();
            ScenarioMetrics m = new ScenarioMetrics(
                    sums[0] / n, sums[1] / n, sums[2] / n,
                    sums[3] / n, sums[4] / n, sums[5] / n,
                    measureLatency ? sums[6] / n : 0.0
            );
            metricsMap.put(scenarioName, m);
        }

        log.info("\n==================================================================================");
        log.info("📊 최종 결과");
        log.info("==================================================================================");

        scenarios.forEach((name, props) ->
                printResult(name, props, metricsMap.get(name), measureLatency));

        log.info("==================================================================================");
        log.info("🏁 {} 종료", phaseName);
        log.info("==================================================================================");

        return metricsMap;
    }

    protected void saveReport(String fileName, String phaseName,
                              Map<String, ScenarioMetrics> metricsMap, int totalQueries) throws IOException {
        List<Map<String, Object>> scenarioList = new ArrayList<>();
        for (Map.Entry<String, ScenarioMetrics> e : metricsMap.entrySet()) {
            ScenarioMetrics m = e.getValue();
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("scenarioName", e.getKey());
            s.put("averageNDCG4", round4(m.ndcg4()));
            s.put("averageNDCG8", round4(m.ndcg8()));
            s.put("averageNDCG20", round4(m.ndcg20()));
            s.put("averageRecall4", round4(m.recall4()));
            s.put("averageRecall8", round4(m.recall8()));
            s.put("averageRecall20", round4(m.recall20()));
            s.put("avgLatencyMs", round2(m.avgLatencyMs()));
            scenarioList.add(s);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("evaluatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("groundTruthFile", "generated-search-ground-truth.json");
        report.put("totalQueries", totalQueries);
        report.put("phaseName", phaseName);
        report.put("scenarios", scenarioList);

        ObjectMapper writer = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        File outputFile = new File("src/test/resources/" + fileName);
        writer.writeValue(outputFile, report);
        log.info("✅ 리포트 저장 완료: {}", outputFile.getAbsolutePath());
    }

    protected GeneralSearchProperties createProperties(
            float titleBoost, float summaryBoost, float chunkBoost,
            int k, int candidates) {

        GeneralSearchProperties props = new GeneralSearchProperties();
        props.setSearchSize(20);

        props.setTitleBoost(titleBoost);
        props.setSummaryBoost(summaryBoost);
        props.setBm25ChunkBoost(chunkBoost);
        props.setVectorChunkBoost(chunkBoost);

        props.setKnnK(k);
        props.setKnnNumCandidates(candidates);

        props.setPersonalScoreWeight(1.0);
        props.setRRF_WINDOW_SIZE(60);

        return props;
    }

    /**
     * Phase 5용 오버로드: dis_max 구조 파라미터 (exactBoost, tieBreaker, bm25ChunkBoost, vectorChunkBoost) 지원
     */
    protected GeneralSearchProperties createProperties(
            float titleBoost, float summaryBoost, float bm25ChunkBoost, float vectorChunkBoost,
            float exactBoost, float fuzzyBoost, float tieBreaker,
            int k, int candidates) {

        GeneralSearchProperties props = new GeneralSearchProperties();
        props.setSearchSize(20);

        props.setTitleBoost(titleBoost);
        props.setSummaryBoost(summaryBoost);
        props.setBm25ChunkBoost(bm25ChunkBoost);
        props.setVectorChunkBoost(vectorChunkBoost);

        props.setExactBoost(exactBoost);
        props.setFuzzyBoost(fuzzyBoost);
        props.setTieBreaker(tieBreaker);

        props.setKnnK(k);
        props.setKnnNumCandidates(candidates);

        props.setPersonalScoreWeight(1.0);
        props.setRRF_WINDOW_SIZE(60);

        return props;
    }

    private void printResult(String name, GeneralSearchProperties p, ScenarioMetrics m, boolean showLatency) {
        System.out.println("\n#######################################################################################");
        System.out.println("▶ " + name);
        System.out.printf("   [Field ] Title:%.2f | Summary:%.2f | BM25Chunk:%.2f | VecChunk:%.2f%n",
                p.getTitleBoost(), p.getSummaryBoost(), p.getBm25ChunkBoost(), p.getVectorChunkBoost());
        System.out.printf("   [BM25 ] exact:%.2f | fuzzy:%.2f | tieBreaker:%.2f%n",
                p.getExactBoost(), p.getFuzzyBoost(), p.getTieBreaker());
        System.out.printf("   [KNN  ] k=%d | candidates=%d%n",
                p.getKnnK(), p.getKnnNumCandidates());
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.printf("   nDCG@4  : %.4f   nDCG@8  : %.4f    nDCG@20 : %.4f%n",
                m.ndcg4(), m.ndcg8(), m.ndcg20());
        System.out.printf("   Recall@4: %.4f   Recall@8: %.4f    Recall@20: %.4f%n",
                m.recall4(), m.recall8(), m.recall20());
        if (showLatency) {
            System.out.printf("   Latency : %.2f ms%n", m.avgLatencyMs());
        }
        System.out.println("#######################################################################################");
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
