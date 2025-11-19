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

    /** * 1단계 검색의 최종 결과 개수 (Elasticsearch size).
     */
    private Integer searchSize = 15;

    /** * BM25 키워드 검색 시 title 필드에 적용되는 가중치 (Boost).
     */
    private Float titleBoost = 2.0f;

    /** * BM25 키워드 검색 시 summary 필드에 적용되는 가중치 (Boost).
     */
    private Float summaryBoost = 2.0f;

    /** * k-NN 검색 시 반환할 이웃 수 (k).
     */
    private Integer knnK = 15;

    /** * k-NN 검색 시 탐색할 후보군 수 (num_candidates).
     */
    private Integer knnNumCandidates = 30;

    /** * ContentChunks BM25 쿼리 결과에 적용되는 가중치 (Boost).
     */
    private Float chunkBoost = 1.0f;

    /**
     * k-NN Script Score (벡터 유사도) 결과에 적용되는 가중치 (Weight).
     */
    private Float semanticBoost = 5.0f;
}