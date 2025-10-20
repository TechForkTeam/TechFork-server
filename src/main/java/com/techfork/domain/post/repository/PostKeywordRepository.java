package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.PostKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostKeywordRepository extends JpaRepository<PostKeyword, Long> {
}
