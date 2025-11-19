package com.techfork.domain.search.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchResult {
    private Long postId;
    private String title;
    private String summary;
    private String companyName;
    private String url;
    private String logoUrl;

    private double hybridScore;
    private double personalScore;
    private double finalScore;

    private float[] documentVector;
}