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

    // 최종 사용자에게 보여줄 결과 개수
    private Integer searchSize = 20;

    // BM25 가중치
    private Float titleBoost = 3.0f;
    private Float summaryBoost = 1.0f;
    private Float chunkBoost = 0.5f;

    // --- [Vector & KNN 설정] ---

    // KNN 검색 시 가져올 개수.
    private Integer knnK = 60;

    private Integer knnNumCandidates = 200;
    private Float vectorTitleBoost = 3.0f;
    private Float vectorSummaryBoost = 1.5f;
    private Float vectorContentChunkBoost = 0.8f;

    // --- [RRF 및 가중치 설정] ---

    // 1차 검색 점수(RRF) 가중치
    private double hybridScoreWeight = 50.0;

    // 2차 개인화 점수 가중치
    private double personalScoreWeight = 1.0;

    // RRF 상수 (k).
    static final int RRF_K = 60;

    // RRF 계산을 위해 각 쿼리(BM25, Vector)에서 가져올 상위 문서 수
    static final int RRF_WINDOW_SIZE = 60;
}