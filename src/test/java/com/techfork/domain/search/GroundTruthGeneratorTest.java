package com.techfork.domain.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.techfork.domain.search.dto.SearchResult;
import com.techfork.domain.search.service.GeneralSearchProperties;
import com.techfork.domain.search.service.SearchServiceImpl;
import com.techfork.domain.user.repository.UserProfileDocumentRepository;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.llm.EmbeddingClient;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * ground-truth.json 테스트
 * - default 하이퍼파라미터 값 적용됨.
 */
@SpringBootTest
class GroundTruthGeneratorTest {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private GeneralSearchProperties generalSearchProperties;

    @Autowired
    private UserProfileDocumentRepository userProfileDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Executor searchAsyncExecutor;

    @Test
    @DisplayName("실제 DB 데이터를 기반으로 ground-truth.json 템플릿 생성")
    void generateGroundTruthTemplate() {
        List<String> keywords = List.of("스프링", "Java", "Spring Boot", "JPA", "Docker", "MSA", "배치", "클라우드");

        SearchServiceImpl searchService = new SearchServiceImpl(
                elasticsearchClient, embeddingClient, generalSearchProperties, userProfileDocumentRepository, userRepository, searchAsyncExecutor);

        System.out.println("========== [Copy Below JSON] ==========");
        System.out.println("[");

        for (int i = 0; i < keywords.size(); i++) {
            String query = keywords.get(i);
            List<SearchResult> results = searchService.searchGeneral(query);

            if (results.isEmpty()) continue;

            System.out.println("  {");
            System.out.printf("    \"query\": \"%s\",\n", query);
            System.out.println("    \"idealResultsMap\": {");

            for (int j = 0; j < Math.min(results.size(), 3); j++) {
                SearchResult result = results.get(j);
                int score = (j == 0) ? 5 : (j == 1 ? 3 : 1);

                String comma = (j == Math.min(results.size(), 3) - 1) ? "" : ",";
                System.out.printf("      \"%s\": %d%s  // %s\n",
                        result.getPostId(), score, comma, result.getTitle().replace("\"", "'"));
            }

            System.out.println("    }");
            if (i == keywords.size() - 1) {
                System.out.println("  }");
            } else {
                System.out.println("  },");
            }
        }
        System.out.println("]");
        System.out.println("=======================================");
    }
}