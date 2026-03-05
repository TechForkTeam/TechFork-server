package com.techfork.domain.search.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "search.general")
public class GeneralSearchProperties {

    // --- [기본 검색 설정] ---

    // 최종 검색 결과 개수
    private Integer searchSize = 20;

    // BM25 가중치
    private Float exactBoost = 2.0f;
    private Float titleBoost = 3.0f;
    private Float summaryBoost = 1.5f;
    private Float fuzzyBoost = 1.0f;
    private Float chunkBoost = 1.0f;

    // --- [Vector & KNN 설정] ---

    private Integer knnK = 40;
    private Integer knnNumCandidates = 50;

    // --- [RRF 및 가중치 설정] ---

    private double hybridScoreWeight = 50.0;
    private double personalScoreWeight = 1.0;
    private int RRF_WINDOW_SIZE = 40;

    // --- [rerank 가중치 설정] ---
    private double rerankDocumentTitleWeight = 0.6;
    private double rerankDocumentSummaryWeight = 0.4;
}