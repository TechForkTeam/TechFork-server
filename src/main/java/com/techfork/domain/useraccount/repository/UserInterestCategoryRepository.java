package com.techfork.domain.useraccount.repository;

import com.techfork.domain.useraccount.entity.UserInterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserInterestCategoryRepository extends JpaRepository<UserInterestCategory, Long> {
    @Query("""
            SELECT DISTINCT uic FROM UserInterestCategory uic
            LEFT JOIN FETCH uic.keywords
            WHERE uic.user.id = :userId
            """)
    List<UserInterestCategory> findByUserIdWithKeywords(@Param("userId") Long userId);

}
