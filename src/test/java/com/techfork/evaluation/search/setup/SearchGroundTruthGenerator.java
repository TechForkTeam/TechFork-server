package com.techfork.evaluation.search.setup;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.techfork.activity.bookmark.repository.BookmarkRepository;
import com.techfork.domain.post.document.PostDocument;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.search.config.GeneralSearchProperties;
import com.techfork.domain.search.service.SearchServiceImpl;
import com.techfork.domain.personalization.repository.PersonalizationProfileDocumentRepository;
import com.techfork.evaluation.recommendation.setup.components.FileExporter;
import com.techfork.evaluation.search.util.GroundTruthItem;
import com.techfork.global.config.CloudflareThirdPartyThumbnailOptimizationProperties;
import com.techfork.global.llm.EmbeddingClient;
import com.techfork.global.llm.LlmClient;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 검색 평가용 ground-truth 데이터를 LLM으로 자동 생성하는 도구.
 *
 * <p>동작 흐름:
 * <ol>
 *   <li>ES "posts" 인덱스에서 company별 층화 샘플링 (약 200~300개 문서)</li>
 *   <li>각 문서에 대해 LLM으로 검색 쿼리 3개 생성 (단일/복합/자연어)</li>
 *   <li>TREC Pooling: BM25 only / Vector only / Hybrid 3종 검색 결과를 union해 후보 풀 구성</li>
 *   <li>LLM-as-a-Judge로 각 (쿼리, 문서) 쌍의 관련도 0~3점 평가</li>
 *   <li>결과를 {@code fixtures/evaluation/generated-search-ground-truth.json}으로 저장</li>
 * </ol>
 *
 * <p>실행 조건:
 * <ul>
 *   <li>로컬 터널 환경 필요: {@code -Dspring.profiles.active=local-tunnel}</li>
 *   <li>LLM 호출 비용 발생 (약 240문서 × 3쿼리 × ~2배치(20개씩) = 최대 ~1,440 LLM 호출)</li>
 *   <li>llmSummary 레이트리미터 적용 (37 req/min) — 실행 시간 수 시간 소요</li>
 * </ul>
 */
@Tag("evaluation-setup")
@Disabled("ground-truth 데이터 생성용 - 수동 실행 (EnvFile 플러그인으로 .env 로드 필요)")
@ActiveProfiles("local-tunnel")
@Slf4j
@SpringBootTest
class SearchGroundTruthGenerator {

    private static final int DOCS_PER_COMPANY = 5;
    private static final int TOP_N_SEARCH_RESULTS = 20;
    private static final int SCORE_THRESHOLD = 1;

    private static final String QUERY_GEN_SYSTEM_PROMPT = """
            너는 시니어 백엔드 개발자이자 검색 쿼리 설계 전문가야.
            한국 개발자가 기술 블로그를 검색할 때 실제로 입력할 법한 쿼리 3개를 생성해.

            [생성 규칙]
            1. 단일 키워드: 핵심 기술/개념 1개 (예: "Docker", "Redis")
            2. 복합 키워드: 기술 + 행동/문제 조합 (예: "Spring Boot JPA 최적화")
            3. 자연어 질문: 실제 궁금증 형태 (예: "쿠버네티스 배포 자동화 방법")

            반드시 아래 JSON 배열만 출력해. 다른 설명은 절대 추가하지 마.
            ["쿼리1", "쿼리2", "쿼리3"]
            """;

    private static final String JUDGE_SYSTEM_PROMPT = """
            너는 시니어 백엔드 개발자이자 정보 검색(IR) 평가 전문가야.
            사용자의 검색 쿼리 의도를 파악하고, 제공된 기술 블로그 게시글 목록이 쿼리와 얼마나 관련 있는지 평가해.

            [평가 기준]
            3점 (Perfect): 쿼리 의도를 완벽히 해결하는 핵심 정답글 (문제 해결 코드, 정확한 원인 분석 포함)
            2점 (Relevant): 부분적으로 도움이 되거나 관련 기술의 기본 개념을 잘 설명하는 글
            1점 (Marginal): 검색어 키워드는 포함되어 있으나 쿼리의 핵심 의도와는 거리가 있는 글
            0점 (Irrelevant): 전혀 관련 없거나 다른 기술을 설명하는 글

            반드시 아래 JSON 배열만 출력해. 다른 설명은 절대 추가하지 마.
            [{"postId": 123, "score": 2, "reason": "10자 이내 사유"}, ...]
            """;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private GeneralSearchProperties generalSearchProperties;

    @Autowired
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    @Qualifier("searchAsyncExecutor")
    private Executor searchAsyncExecutor;

    @Autowired
    private FileExporter fileExporter;

    private record QueryGenerationResult(String query, Long sourcePostId) {}

    @Test
    @DisplayName("LLM으로 검색 평가용 ground-truth 데이터 자동 생성")
    void generateSearchGroundTruth() throws IOException {
        // Step 1: ES에서 company별 층화 샘플링
        log.info("=== [Step 1] ES 문서 층화 샘플링 시작 ===");
        List<PostDocument> sampledDocs = sampleDocumentsStratified();
        log.info("샘플링 완료: 총 {} 개 문서", sampledDocs.size());

        // Step 2: 각 문서에 대해 LLM으로 쿼리 생성
        log.info("=== [Step 2] LLM 쿼리 생성 시작 ===");
        List<QueryGenerationResult> allQueryResults = generateAllQueries(sampledDocs);

        // 중복 쿼리 제거 (동일 쿼리가 여러 문서에서 생성된 경우 첫 번째 유지)
        Map<String, Long> uniqueQueryMap = new LinkedHashMap<>();
        for (QueryGenerationResult qr : allQueryResults) {
            uniqueQueryMap.putIfAbsent(qr.query(), qr.sourcePostId());
        }
        log.info("생성된 쿼리: {} 개 → 중복 제거 후: {} 개", allQueryResults.size(), uniqueQueryMap.size());

        // Step 3 & 4: 각 쿼리로 검색 실행 + LLM 관련도 평가
        log.info("=== [Step 3 & 4] 검색 실행 및 LLM 관련도 평가 시작 ===");
        CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer = new CloudflareThirdPartyThumbnailOptimizer(
                new CloudflareThirdPartyThumbnailOptimizationProperties()
        );
        SearchServiceImpl searchService = new SearchServiceImpl(
                elasticsearchClient, embeddingClient, generalSearchProperties,
                personalizationProfileDocumentRepository, postRepository, bookmarkRepository, searchAsyncExecutor, thumbnailOptimizer);

        List<GroundTruthItem> groundTruthItems = scoreAllQueries(uniqueQueryMap, searchService);
        log.info("최종 ground-truth 항목 수: {}", groundTruthItems.size());

        // Step 5: 파일 저장
        log.info("=== [Step 5] 결과 저장 ===");
        fileExporter.ensureOutputDirectory();
        fileExporter.writeJsonFile("generated-search-ground-truth.json", groundTruthItems);
        log.info("저장 완료: src/test/resources/fixtures/evaluation/generated-search-ground-truth.json");
    }

    // -------------------------------------------------------------------------
    // Step 1: 층화 샘플링
    // -------------------------------------------------------------------------

    private List<PostDocument> sampleDocumentsStratified() {
        List<String> companies = postRepository.findDistinctCompanies();
        log.info("전체 회사 수: {}", companies.size());

        List<PostDocument> result = new ArrayList<>();
        for (String company : companies) {
            try {
                SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                        .index("posts")
                        .size(DOCS_PER_COMPANY)
                        .query(q -> q
                                .term(t -> t
                                        .field("company")
                                        .value(company)
                                )
                        ),
                        PostDocument.class
                );
                response.hits().hits().stream()
                        .map(hit -> hit.source())
                        .filter(Objects::nonNull)
                        .forEach(result::add);

                log.debug("회사 '{}': {} 개 문서 샘플링", company, response.hits().hits().size());
            } catch (Exception e) {
                log.warn("회사 '{}' 샘플링 실패: {}", company, e.getMessage());
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Step 2: LLM 쿼리 생성
    // -------------------------------------------------------------------------

    private List<QueryGenerationResult> generateAllQueries(List<PostDocument> docs) {
        List<QueryGenerationResult> allResults = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            if (i % 10 == 0) {
                log.info("쿼리 생성 진행 중: {}/{}", i, docs.size());
            }
            PostDocument doc = docs.get(i);
            try {
                List<String> queries = generateQueriesForDocument(doc);
                for (String query : queries) {
                    allResults.add(new QueryGenerationResult(query, doc.getPostId()));
                }
            } catch (Exception e) {
                log.warn("문서 postId={} 쿼리 생성 실패: {}", doc.getPostId(), e.getMessage());
            }
        }
        return allResults;
    }

    private List<String> generateQueriesForDocument(PostDocument doc) {
        String userPrompt = String.format("""
                제목: %s
                회사: %s
                요약: %s
                """,
                doc.getTitle(),
                doc.getCompany(),
                doc.getSummary() != null ? doc.getSummary() : "(요약 없음)"
        );

        String response = llmClient.call(QUERY_GEN_SYSTEM_PROMPT, userPrompt);
        return parseJsonArray(response);
    }

    // -------------------------------------------------------------------------
    // Step 3 & 4: 검색 실행 + LLM 관련도 평가
    // -------------------------------------------------------------------------

    private List<GroundTruthItem> scoreAllQueries(
            Map<String, Long> uniqueQueryMap,
            SearchServiceImpl searchService) {

        List<GroundTruthItem> items = new ArrayList<>();
        int queryCount = 0;

        for (String query : uniqueQueryMap.keySet()) {
            if (queryCount % 10 == 0) {
                log.info("관련도 평가 진행 중: {}/{}", queryCount, uniqueQueryMap.size());
            }
            queryCount++;

            try {
                // TREC Pooling: 3종 검색 결과를 union해 후보 풀 구성
                List<SearchResult> bm25Results = searchService.searchOnlyBm25(query);
                List<SearchResult> vectorResults = searchService.searchOnlySemantic(query);
                List<SearchResult> hybridResults = searchService.searchGeneral(query);

                Map<Long, SearchResult> poolMap = new LinkedHashMap<>();
                for (SearchResult r : bm25Results.subList(0, Math.min(bm25Results.size(), TOP_N_SEARCH_RESULTS))) {
                    poolMap.putIfAbsent(r.getPostId(), r);
                }
                for (SearchResult r : vectorResults.subList(0, Math.min(vectorResults.size(), TOP_N_SEARCH_RESULTS))) {
                    poolMap.putIfAbsent(r.getPostId(), r);
                }
                for (SearchResult r : hybridResults.subList(0, Math.min(hybridResults.size(), TOP_N_SEARCH_RESULTS))) {
                    poolMap.putIfAbsent(r.getPostId(), r);
                }
                List<SearchResult> candidatePool = new ArrayList<>(poolMap.values());
                log.debug("쿼리 '{}': BM25={}, Vector={}, Hybrid={} → union={}", query,
                        bm25Results.size(), vectorResults.size(), hybridResults.size(), candidatePool.size());

                Map<String, Integer> idealResultsMap = new LinkedHashMap<>();
                try {
                    Map<Long, Integer> batchScores = scoreBatch(query, candidatePool);
                    batchScores.forEach((postId, score) -> {
                        if (score > SCORE_THRESHOLD) {
                            idealResultsMap.put(String.valueOf(postId), score);
                        }
                    });
                } catch (Exception e) {
                    log.warn("배치 관련도 평가 실패 (query='{}'): {}", query, e.getMessage());
                }

                if (!idealResultsMap.isEmpty()) {
                    GroundTruthItem item = new GroundTruthItem();
                    item.setQuery(query);
                    item.setIdealResultsMap(idealResultsMap);
                    items.add(item);
                }
            } catch (Exception e) {
                log.warn("쿼리 '{}' 처리 실패: {}", query, e.getMessage());
            }
        }
        return items;
    }

    private static final int BATCH_SIZE = 20;

    /**
     * 후보 문서 목록을 BATCH_SIZE 단위로 나눠 LLM에 일괄 평가 요청.
     * 호출 수: ceil(candidatePool.size() / BATCH_SIZE)
     */
    private Map<Long, Integer> scoreBatch(String query, List<SearchResult> candidatePool) {
        Map<Long, Integer> scores = new LinkedHashMap<>();
        List<List<SearchResult>> batches = partition(candidatePool, BATCH_SIZE);

        for (List<SearchResult> batch : batches) {
            StringBuilder docsJson = new StringBuilder();
            for (SearchResult r : batch) {
                docsJson.append(String.format(
                        """
                        {"postId": %d, "title": "%s", "company": "%s", "summary": "%s"},
                        """,
                        r.getPostId(),
                        escape(r.getTitle()),
                        escape(r.getCompanyName()),
                        escape(r.getSummary() != null ? r.getSummary() : "")
                ));
            }

            String userPrompt = String.format("""
                    [검색 쿼리]: %s

                    [평가할 문서 목록]
                    [%s]
                    """,
                    query,
                    docsJson
            );

            try {
                String response = llmClient.call(JUDGE_SYSTEM_PROMPT, userPrompt);
                parseBatchScores(response, scores);
            } catch (Exception e) {
                log.warn("배치 LLM 호출 실패 (query='{}', batchSize={}): {}", query, batch.size(), e.getMessage());
            }
        }
        return scores;
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private void parseBatchScores(String response, Map<Long, Integer> scores) {
        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start == -1 || end == -1 || start >= end) {
                log.warn("배치 점수 파싱 실패 - JSON 배열 없음: {}", response);
                return;
            }
            JsonNode array = objectMapper.readTree(response.substring(start, end + 1));
            for (JsonNode node : array) {
                if (!node.has("postId") || !node.has("score")) continue;
                long postId = node.get("postId").asLong();
                int score = Math.max(0, Math.min(3, node.get("score").asInt()));
                scores.put(postId, score);
            }
        } catch (Exception e) {
            log.warn("배치 점수 파싱 실패: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 파싱 헬퍼
    // -------------------------------------------------------------------------

    private List<String> parseJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start == -1 || end == -1 || start >= end) {
            log.warn("JSON 배열 파싱 실패 (괄호 없음): {}", response);
            return Collections.emptyList();
        }
        String content = response.substring(start + 1, end);
        // 따옴표 내부의 쉼표를 보존하기 위해 따옴표 밖의 쉼표로만 분리
        return Arrays.stream(content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }


}
