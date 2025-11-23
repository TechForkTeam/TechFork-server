package com.techfork.domain.recommendation.repository;

import com.techfork.domain.recommendation.entity.RecommendationHistory;
import com.techfork.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {

    /**
     * 특정 기간 내 사용자의 추천 이력 조회
     */
    @Query("""
            SELECT rh FROM RecommendationHistory rh
            WHERE rh.user = :user
            AND rh.recommendedAt >= :startDate
            AND rh.recommendedAt < :endDate
            ORDER BY rh.recommendedAt DESC, rh.rankOrder ASC
            """)
    List<RecommendationHistory> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 사용자의 최근 추천 이력 조회
     */
    @Query("""
            SELECT rh FROM RecommendationHistory rh
            WHERE rh.user = :user
            ORDER BY rh.recommendedAt DESC, rh.rankOrder ASC
            """)
    List<RecommendationHistory> findRecentByUser(@Param("user") User user, Pageable pageable);

    /**
     * 클릭된 추천 이력 조회 (CTR 분석용)
     */
    @Query("""
            SELECT rh FROM RecommendationHistory rh
            WHERE rh.user = :user
            AND rh.clicked = true
            ORDER BY rh.clickedAt DESC
            """)
    List<RecommendationHistory> findClickedRecommendations(@Param("user") User user, Pageable pageable);

    /**
     * 특정 날짜의 전체 추천 통계
     */
    @Query("""
            SELECT COUNT(rh),
                   SUM(CASE WHEN rh.clicked = true THEN 1 ELSE 0 END),
                   AVG(rh.mmrScore)
            FROM RecommendationHistory rh
            WHERE rh.recommendedAt >= :startDate
            AND rh.recommendedAt < :endDate
            """)
    Object[] getRecommendationStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 오래된 이력 삭제 (N일 이상)
     */
    void deleteByArchivedAtBefore(LocalDateTime before);
}
