package com.techfork.global.configuration;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestConfig {

    private static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7.2-alpine"));

    static {
        redis.start();
    }

    @Bean
    @ServiceConnection
    RedisContainer redisContainer() {
        return redis;
    }
}
