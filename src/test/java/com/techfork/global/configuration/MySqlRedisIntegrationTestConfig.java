package com.techfork.global.configuration;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Elasticsearch가 필요 없는 로컬 통합 테스트용 설정.
 *
 * <p>기본 {@link IntegrationTestConfig}는 Elasticsearch + MySQL + Redis를 모두 띄우므로
 * 로컬에서 Activity/Auth/UserAccount 같은 DB/Redis 위주 테스트를 빠르게 돌릴 때는
 * 이 설정을 사용하도록 테스트 베이스 상속만 수동으로 바꿔서 사용한다.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlRedisIntegrationTestConfig {

    private static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0");

    private static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7.2-alpine"));

    static {
        mysql.start();
        redis.start();
    }

    @Bean
    @ServiceConnection
    MySQLContainer<?> mySQLContainer() {
        return mysql;
    }

    @Bean
    @ServiceConnection
    RedisContainer redisContainer() {
        return redis;
    }
}
