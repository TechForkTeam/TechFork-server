package com.techfork.global.config;

import io.netty.channel.ChannelOption;
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
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000) // 연결 타임아웃
                .responseTimeout(Duration.ofSeconds(30)) // 응답 타임아웃
                .followRedirect(true); // Redirect 자동 추적

        // WebClient 생성
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; TechFork-Bot/1.0)") // 봇 차단 방지
                .defaultHeader("Accept", "application/rss+xml, application/xml, application/atom+xml, text/xml, */*") // RSS/XML 콘텐츠 명시
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 큰 RSS 피드 처리 가능
                .build();
    }
}
