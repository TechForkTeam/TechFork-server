package com.techfork.domain.user.repository;

import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.entity.UserInterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInterestCategoryRepository extends JpaRepository<UserInterestCategory, Long> {

    void deleteByUser(User user);
}
