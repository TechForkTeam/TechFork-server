package com.techfork.useraccount.infrastructure;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.interestCategories
            WHERE u.id = :userId
            """)
    Optional<User> findByIdWithInterestCategories(@Param("userId") Long userId);

    /**
     * 최근 특정 시간 이후 활동한 사용자 조회
     * (읽은 포스트, 북마크, 검색 기록 중 하나라도 있으면 활성 사용자)
     * 탈퇴한 사용자는 제외
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE u.status != 'WITHDRAWN'
            AND (EXISTS (
                SELECT 1 FROM ReadPost rp WHERE rp.user = u AND rp.readAt >= :since
            ) OR EXISTS (
                SELECT 1 FROM Bookmark b WHERE b.user = u AND b.bookmarkedAt >= :since
            ) OR EXISTS (
                SELECT 1 FROM SearchHistory sh WHERE sh.user = u AND sh.searchedAt >= :since
            ))
            """)
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * 관심사 카테고리와 함께 사용자 조회 (Fetch Join)
     * 주의: keywords는 Multiple Bag Fetch 문제로 제외 (필요시 별도 쿼리)
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.interestCategories
            WHERE u.id IN :userIds
            """)
    List<User> findAllWithInterestCategoriesByIds(@Param("userIds") List<Long> userIds);
}
