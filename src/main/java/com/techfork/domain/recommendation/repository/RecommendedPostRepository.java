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


    @Query("SELECT rp FROM RecommendedPost rp WHERE rp.user = :user AND rp.recommendedAt < :before")
    List<RecommendedPost> findByUserAndRecommendedAtBefore(@Param("user") User user, @Param("before") LocalDateTime before);


    @Modifying
    @Query("DELETE FROM RecommendedPost rp WHERE rp.user = :user AND rp.recommendedAt < :before")
    void deleteByUserAndRecommendedAtBefore(@Param("user") User user, @Param("before") LocalDateTime before);

    @Query("""
           SELECT rp FROM RecommendedPost rp 
           JOIN FETCH rp.post p 
           JOIN FETCH p.techBlog
           WHERE rp.user = :user 
           ORDER BY rp.rankOrder ASC
           """)
    List<RecommendedPost> findByUserOrderByRankAsc(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM RecommendedPost rp WHERE rp.user = :user")
    void deleteByUser(@Param("user") User user);
}
