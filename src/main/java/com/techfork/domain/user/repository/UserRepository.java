package com.techfork.domain.user.repository;

import com.techfork.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 최근 특정 시간 이후 활동한 사용자 조회
     * (읽은 포스트, 스크랩, 검색 기록 중 하나라도 있으면 활성 사용자)
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE EXISTS (
                SELECT 1 FROM ReadPost rp WHERE rp.user = u AND rp.readAt >= :since
            ) OR EXISTS (
                SELECT 1 FROM ScrabPost sp WHERE sp.user = u AND sp.scrappedAt >= :since
            ) OR EXISTS (
                SELECT 1 FROM SearchHistory sh WHERE sh.user = u AND sh.searchedAt >= :since
            )
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
