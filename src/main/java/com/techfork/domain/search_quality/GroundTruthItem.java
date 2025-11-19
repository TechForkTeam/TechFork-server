package com.techfork.domain.search_quality;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class GroundTruthItem {

    private String query;
    private Map<String, Integer> idealResultsMap;
}