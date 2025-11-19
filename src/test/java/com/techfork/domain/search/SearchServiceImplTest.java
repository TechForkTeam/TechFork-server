package com.techfork.domain.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.search.service.GeneralSearchProperties;
import com.techfork.domain.search.service.SearchServiceImpl;
import com.techfork.global.llm.EmbeddingClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class SearchServiceImplTest {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Test
    @DisplayName("다양한 하이퍼파라미터 시나리오별 검색 결과 비교 테스트")
    void compareSearchScenarios() {
        String query = "스프링 배치";

        Map<String, GeneralSearchProperties> scenarios = createScenarios();

        scenarios.forEach((name, props) -> {
            System.out.println("\n##########################################################");
            System.out.println("▶ 시나리오 실행: " + name);
            System.out.printf("   [설정] Title:%.1f | Summary:%.1f | Chunk:%.1f | Semantic:%.1f | K:%d | Candidates:%d\n",
                    props.getTitleBoost(), props.getSummaryBoost(), props.getChunkBoost(), props.getSemanticBoost(),
                    props.getKnnK(), props.getKnnNumCandidates());
            System.out.println("##########################################################");

            runSearch(props, query);
        });
    }

    private Map<String, GeneralSearchProperties> createScenarios() {
        Map<String, GeneralSearchProperties> scenarios = new LinkedHashMap<>();

        // (Signature: Title, Summary, Chunk, Semantic, KNN K, Candidates)
        // -------------------------------------------------------------------------------------------------------

        // 1. Baseline (기본값)
        scenarios.put("1. Baseline (균형 잡힌 기본값)",
                createProperties(2.0f, 1.5f, 1.0f, 5.0f, 15, 30));

        // 2. Title Heavy (제목에 키워드가 있는 문서를 강력하게 밀어줌)
        scenarios.put("2. Title Heavy (제목 집중)",
                createProperties(10.0f, 1.0f, 1.0f, 1.0f, 15, 30));

        // 3. Summary Heavy (요약에 키워드가 있는 문서를 강력하게 밀어줌)
        scenarios.put("3. Summary Heavy (요약 집중)",
                createProperties(1.0f, 10.0f, 1.0f, 1.0f, 15, 30));

        // 4. Content Heavy (본문 상세 내용 집중)
        scenarios.put("4. Content Heavy (본문 Chunk 집중)",
                createProperties(1.0f, 1.0f, 5.0f, 1.0f, 15, 30));

        // 5. Semantic Heavy (의미/문맥 집중)
        scenarios.put("5. Semantic Heavy (벡터 유사도 집중)",
                createProperties(1.0f, 1.0f, 1.0f, 20.0f, 50, 100));

        // 6. Keyword Only (시맨틱 무시)
        scenarios.put("6. Keyword Only (Vector 무시)",
                createProperties(5.0f, 5.0f, 5.0f, 0.0f, 15, 30));

        // 7. Title + Summary Focused (키워드 Precision 극대화)
        // 본문 청크를 무시하고 제목과 요약에만 집중
        scenarios.put("7. Title+Summary Focused (Precision)",
                createProperties(5.0f, 5.0f, 0.1f, 5.0f, 15, 30));

        // 8. Title + Semantic (제목과 의미 조합)
        // 요약이나 본문 상세보다 제목 일치와 의미가 더 중요함
        scenarios.put("8. Title + Semantic (제목+의미)",
                createProperties(8.0f, 1.0f, 1.0f, 10.0f, 20, 40));

        // 9. High Recall (탐색 범위 확장)
        // 가중치는 기본이지만 k-NN 후보군을 대폭 늘려 더 넓은 범위에서 유사한 문서를 탐색
        scenarios.put("9. High Recall (탐색 범위 확장)",
                createProperties(2.0f, 1.5f, 1.0f, 5.0f, 100, 200));

        // 10. Title Low, Summary/Chunk High (제목보다 내용이 중요)
        scenarios.put("10. Content & Summary Priority",
                createProperties(0.5f, 5.0f, 5.0f, 5.0f, 20, 40));

        return scenarios;
    }

    private GeneralSearchProperties createProperties(float titleBoost, float summaryBoost, float chunkBoost, float semanticBoost, int k, int candidates) {
        GeneralSearchProperties props = new GeneralSearchProperties();
        props.setSearchSize(5);
        props.setTitleBoost(titleBoost);
        props.setSummaryBoost(summaryBoost);
        props.setChunkBoost(chunkBoost);
        props.setSemanticBoost(semanticBoost);
        props.setKnnK(k);
        props.setKnnNumCandidates(candidates);
        return props;
    }

    private void runSearch(GeneralSearchProperties props, String query) {
        SearchServiceImpl searchService = new SearchServiceImpl(
                elasticsearchClient,
                embeddingClient,
                props
        );

        try {
            long startTime = System.currentTimeMillis();
            List<SearchResult> results = searchService.searchGeneral(query);
            long endTime = System.currentTimeMillis();

            System.out.println("   > 검색 소요 시간: " + (endTime - startTime) + "ms");

            if (results.isEmpty()) {
                System.out.println("   > 검색 결과가 없습니다.");
            } else {
                for (int i = 0; i < results.size(); i++) {
                    SearchResult result = results.get(i);
                    System.out.printf("   [%d위] 점수: %.4f | 제목: %s\n",
                            (i + 1), result.getFinalScore(), result.getTitle());

                    String summary = result.getSummary() != null
                            ? result.getSummary().replace("\n", " ")
                            : "";
                    if (summary.length() > 100) summary = summary.substring(0, 100) + "...";

                    System.out.println("       - 요약: " + summary);

                    String url = result.getUrl();

                    System.out.println("       - url: " + url);
                }
            }
        } catch (Exception e) {
            System.out.println("   > 검색 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}