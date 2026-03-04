package com.techfork.evaluation.search;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class GroundTruthItem {

    private String query;
    private Map<String, Integer> idealResultsMap;
}