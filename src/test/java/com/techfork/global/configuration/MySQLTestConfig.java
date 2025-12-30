package com.techfork.global.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class MySQLTestConfig {
    @Bean
    @ServiceConnection
    MySQLContainer<?> mySQLContainer() {
        return new MySQLContainer<>("mysql:8.0.36");
    }
}
