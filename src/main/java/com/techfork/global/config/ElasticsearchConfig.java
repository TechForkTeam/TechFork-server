package com.techfork.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.EnableElasticsearchAuditing;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchAuditing
@EnableElasticsearchRepositories(basePackages = "com.techfork")
public class ElasticsearchConfig {
}