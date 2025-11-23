package com.techfork.domain.recommendation.repository;

import com.techfork.domain.recommendation.entity.RecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {
}
