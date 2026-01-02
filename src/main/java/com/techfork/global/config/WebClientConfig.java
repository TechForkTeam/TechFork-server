package com.techfork.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;


@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // HttpClient 설정 (Netty 기반)
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))
                .followRedirect(true); // Redirect 자동 추적

        // WebClient 생성
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; TechFork-Bot/1.0)")
                .defaultHeader("Accept", "application/rss+xml, application/xml, application/atom+xml, text/xml, */*")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
