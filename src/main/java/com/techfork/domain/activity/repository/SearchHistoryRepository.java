package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    @Query("SELECT sh FROM SearchHistory sh WHERE sh.user.id = :userId ORDER BY sh.searchedAt DESC")
    List<SearchHistory> findRecentSearchHistoriesByUserId(@Param("userId") Long userId, Pageable pageable);
}
