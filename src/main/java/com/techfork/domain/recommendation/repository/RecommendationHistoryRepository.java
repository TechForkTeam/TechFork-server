package com.techfork.domain.recommendation.repository;

import com.techfork.recommendation.domain.RecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {
}
