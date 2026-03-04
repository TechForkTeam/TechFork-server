package com.techfork.global.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexConfig implements ApplicationRunner {

    private static final List<String> INDICES = List.of("posts", "user_profiles");

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void run(ApplicationArguments args) {
        INDICES.forEach(this::applyPreload);
    }

    private void applyPreload(String index) {
        try {
            // store.preload는 static setting이라 reopen=true 필요 (ES가 자동으로 닫았다 열어줌)
            // vec: HNSW 그래프, vem: HNSW 메타데이터
            elasticsearchClient.indices().putSettings(s -> s
                    .index(index)
                    .reopen(true)
                    .settings(is -> is
                            .otherSettings("store.preload", JsonData.of(List.of("vec", "vem")))
                    )
            );
            log.info("[ES Index] preload applied to '{}'", index);
        } catch (Exception e) {
            log.warn("[ES Index] preload failed for '{}': {}", index, e.getMessage());
        }
    }
}