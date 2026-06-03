package com.techfork.activity.readhistory.application.query.lookup;

import com.techfork.activity.readhistory.domain.SearchHistory;
import com.techfork.activity.readhistory.infrastructure.SearchHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchHistoryLookupServiceTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private SearchHistoryLookupService searchHistoryLookupService;

    @Test
    @DisplayName("최근 검색어 목록을 조회한다")
    void getRecentSearchQueries_ReturnsQueries() {
        Long userId = 1L;
        int limit = 30;
        SearchHistory springBatch = searchHistory("Spring Batch");
        SearchHistory elasticsearchVector = searchHistory("Elasticsearch vector");
        given(searchHistoryRepository.findRecentSearchHistoriesByUserId(userId, PageRequest.of(0, limit)))
                .willReturn(List.of(springBatch, elasticsearchVector));

        List<String> result = searchHistoryLookupService.getRecentSearchQueries(userId, limit);

        assertThat(result).containsExactly("Spring Batch", "Elasticsearch vector");
        verify(searchHistoryRepository).findRecentSearchHistoriesByUserId(userId, PageRequest.of(0, limit));
    }

    private SearchHistory searchHistory(String query) {
        SearchHistory searchHistory = mock(SearchHistory.class);
        given(searchHistory.getQuery()).willReturn(query);
        return searchHistory;
    }
}
