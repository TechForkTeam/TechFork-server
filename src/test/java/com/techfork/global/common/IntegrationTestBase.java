package com.techfork.global.common;

import com.techfork.global.configuration.ElasticsearchTestConfig;
import com.techfork.global.configuration.MySQLTestConfig;
import com.techfork.global.configuration.RedisTestConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import({MySQLTestConfig.class, RedisTestConfig.class, ElasticsearchTestConfig.class})
@ActiveProfiles("integrationtest")
public abstract class IntegrationTestBase {
}
