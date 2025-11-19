package com.techfork.global.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;


@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        try {
            // SSL Context 생성 - 모든 인증서 신뢰
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            // HttpClient 설정 (Netty 기반)
            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
                    .responseTimeout(Duration.ofSeconds(10));

            // WebClient 생성
            WebClient webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeader("User-Agent", "TechFork-Bot/1.0")
                    .build();

            log.info("✅ SSL-friendly WebClient 초기화 완료");
            return webClient;

        } catch (SSLException e) {
            log.error("❌ WebClient 초기화 실패", e);
            throw new RuntimeException("WebClient 초기화 실패", e);
        }
    }
}
