package com.techfork.global.configuration;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    private static final ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.0")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m");

    private static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0.36");

    private static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7.2-alpine"));

    static {
        elasticsearch.start();
        mysql.start();
        redis.start();
    }

    @Bean
    @ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
        return elasticsearch;
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
