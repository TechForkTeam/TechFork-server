package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.PostKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostKeywordRepository extends JpaRepository<PostKeyword, Long> {
    @Query("SELECT pk FROM PostKeyword pk WHERE pk.post.id IN :postIds")
    List<PostKeyword> findByPostIdIn(@Param("postIds") List<Long> postIds);
}
