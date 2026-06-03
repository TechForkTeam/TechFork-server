package com.techfork.activity.readhistory.application.query.lookup;

import com.techfork.activity.readhistory.domain.SearchHistory;
import com.techfork.activity.readhistory.infrastructure.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchHistoryLookupService {

    private final SearchHistoryRepository searchHistoryRepository;

    public List<String> getRecentSearchQueries(Long userId, int limit) {
        return searchHistoryRepository.findRecentSearchHistoriesByUserId(userId, PageRequest.of(0, limit))
                .stream()
                .map(SearchHistory::getQuery)
                .toList();
    }
}
