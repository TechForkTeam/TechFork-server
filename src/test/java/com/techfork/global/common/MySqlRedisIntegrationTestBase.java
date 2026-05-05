package com.techfork.global.common;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.techfork.domain.personalization.repository.PersonalizationProfileDocumentRepository;
import com.techfork.domain.post.repository.PostDocumentRepository;
import com.techfork.evaluation.recommendation.RecommendationEvaluationService;
import com.techfork.global.configuration.MySqlRedisIntegrationTestConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Elasticsearch를 쓰지 않는 통합 테스트를 위한 경량 베이스 클래스.
 *
 * <p>기본 통합 테스트는 {@link IntegrationTestBase}를 유지하고,
 * 로컬에서 MySQL/Redis만으로 충분한 테스트를 빠르게 돌릴 때 이 클래스로 상속을 바꿔 사용한다.</p>
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlRedisIntegrationTestConfig.class)
@ActiveProfiles("integrationtest")
public abstract class MySqlRedisIntegrationTestBase {

    /**
     * ES 없는 통합 테스트에서 컨텍스트 로딩 시 함께 스캔되는
     * Elasticsearch 관련 bean/repository를 기본적으로 mock 처리한다.
     */
    @MockitoBean
    private ElasticsearchClient elasticsearchClient;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @MockitoBean
    private PostDocumentRepository postDocumentRepository;

    @MockitoBean
    private RecommendationEvaluationService recommendationEvaluationService;
}
