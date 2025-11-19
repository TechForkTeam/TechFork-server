package com.techfork.domain.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search.general")
public class GeneralSearchProperties {

    /** * 1단계 검색의 최종 결과 개수 (Elasticsearch size).
     * RRF의 rankWindowSize와 동일하게 설정하는 것이 일반적입니다.
     */
    private Integer searchSize = 15;

    /** * BM25 키워드 검색 시 title 필드에 적용되는 가중치 (Boost).
     * 예: "title^2.0"
     */
    private Float titleBoost = 2.0f;

    /** * k-NN 검색 시 반환할 이웃 수 (k).
     * 이 값은 RRF에 전달되는 BM25 결과의 개수를 제한합니다. (searchSize와 동일하게 설정)
     */
    private Integer knnK = 15;

    /** * k-NN 검색 시 탐색할 후보군 수 (num_candidates).
     * 이 값이 높을수록 정확도가 높아지지만, 성능은 느려집니다.
     */
    private Integer knnNumCandidates = 30;

    /** * RRF (Reciprocal Rank Fusion) 랭크 퓨전의 윈도우 크기 (rank_window_size).
     * BM25 및 k-NN 검색 각각에서 몇 개의 결과를 RRF에 사용할지 결정합니다.
     */
//    private Long rrfWindowSize = 100L;

    /** * RRF 상수 K (rank_constant).
     * 이 값이 높을수록 개별 검색 결과의 원래 랭크 점수보다 최종 점수에 영향을 더 많이 미칩니다.
     * 일반적으로 60을 사용합니다.
     */
//    private Long rrfRankConstant = 60L;
}