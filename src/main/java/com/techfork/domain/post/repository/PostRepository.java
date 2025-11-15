package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByUrl(String url);

    @Query("SELECT p FROM Post p WHERE p.summary IS NULL OR p.summary = ''")
    List<Post> findBySummaryIsNull();

    Page<Post> findBySummaryIsNotNullAndEmbeddedAtIsNull(Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.embeddedAt = :embeddedAt WHERE p.id IN :ids")
    void bulkUpdateEmbeddedAt(@Param("ids") List<Long> ids, @Param("embeddedAt") LocalDateTime embeddedAt);
}
