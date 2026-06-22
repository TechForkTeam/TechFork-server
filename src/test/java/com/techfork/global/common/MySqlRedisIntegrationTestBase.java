package com.techfork.global.common;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocumentRepository;
import com.techfork.post.infrastructure.PostDocumentRepository;
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
 * 일반 통합 테스트를 위한 MySQL/Redis 베이스 클래스.
 *
 * <p>{@code integrationTest} 레인에서는 이 베이스를 기본으로 사용한다.
 * Elasticsearch 실기동이 필요한 평가/검색 품질 검증은 별도 레인에서 실행한다.</p>
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
