package com.techfork.domain.search.service;

import com.techfork.domain.search.dto.SearchResult;
import java.util.List;

public interface SearchService {

    List<SearchResult> searchOnlyBm25(String query);

    List<SearchResult> searchOnlySemantic(String query);

    List<SearchResult> searchGeneral(String query);

    List<SearchResult> searchPersonalized(String query, Long userId);
}