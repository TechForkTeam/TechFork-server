package com.techfork.global.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ElasticsearchTestConfig {

    private static final ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.0")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m");

    static {
        elasticsearch.start();
    }

    @Bean
    @ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
        return elasticsearch;
    }
}