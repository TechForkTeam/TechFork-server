package com.techfork.global.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ElasticsearchTestConfig {
    @Bean
    @ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.0")
                .withEnv("xpack.security.enabled", "false");
    }
}