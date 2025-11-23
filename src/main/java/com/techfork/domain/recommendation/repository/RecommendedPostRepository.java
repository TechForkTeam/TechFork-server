package com.techfork.domain.recommendation.repository;

import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommendedPostRepository extends JpaRepository<RecommendedPost, Long> {

    /**
     * 특정 시간 이전의 추천 게시글 조회 (이력 보관용)
     */
    @Query("SELECT rp FROM RecommendedPost rp WHERE rp.user = :user AND rp.recommendedAt < :before")
    List<RecommendedPost> findByUserAndRecommendedAtBefore(@Param("user") User user, @Param("before") LocalDateTime before);

    /**
     * 특정 시간 이전의 추천 게시글 삭제
     */
    @Modifying
    @Query("DELETE FROM RecommendedPost rp WHERE rp.user = :user AND rp.recommendedAt < :before")
    void deleteByUserAndRecommendedAtBefore(@Param("user") User user, @Param("before") LocalDateTime before);
}
