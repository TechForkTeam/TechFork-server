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

    // 최종 사용자에게 보여줄 결과 개수 (Paging limit)
    private Integer searchSize = 10;

    // BM25 가중치 (기존 유지)
    private Float titleBoost = 2.0f;
    private Float summaryBoost = 1.0f; // 본문/요약은 제목보다 약간 낮추는 게 일반적
    private Float chunkBoost = 0.5f;   // 청크는 보조 수단이므로 낮춤

    // --- [Vector & KNN 설정] ---

    // [중요] KNN 검색 시 가져올 개수.
    // RRF Window Size와 동일하게 맞춰야 정확한 순위 산정이 가능함. (15 -> 60)
    private Integer knnK = 60;

    // HNSW 그래프 탐색 후보군. k보다 커야 하며, 보통 100~200 정도가 정확도/속도 밸런스가 좋음.
    private Integer knnNumCandidates = 150;

    // --- [RRF 및 가중치 설정 (핵심 튜닝)] ---

    // 1차 검색 점수(RRF) 가중치: 0.6 -> 30.0
    // 이유: RRF 1등 점수가 고작 약 0.033 (1/61 + 1/61)입니다.
    // 이를 개인화 점수(0.0~1.0)와 대등하게 맞추려면 약 30배 뻥튀기가 필요합니다.
    private double hybridScoreWeight = 30.0;

    // 2차 개인화 점수(Cosine) 가중치: 0.4 -> 1.0
    private double personalScoreWeight = 1.0;

    // RRF 상수 (k).
    static final int RRF_K = 60;

    // RRF 계산을 위해 각 쿼리(BM25, Vector)에서 가져올 상위 문서 수
    static final int RRF_WINDOW_SIZE = 60;
}