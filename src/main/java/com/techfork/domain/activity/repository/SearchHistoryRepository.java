package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

}
