package com.techfork.domain.post.repository;

import com.techfork.domain.post.dto.PostSummaryDto;
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

    @Query("SELECT DISTINCT p.company FROM Post p ORDER BY p.company")
    List<String> findDistinctCompanies();

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostSummaryDto(
            p.id, p.title, p.summary, p.company, p.url, p.publishedAt, p.crawledAt)
            FROM Post p
            WHERE (:company IS NULL OR p.company = :company)
            AND (:lastPostId IS NULL OR p.id < :lastPostId)
            ORDER BY p.id DESC
            """)
    List<PostSummaryDto> findByCompanyWithCursor(
            @Param("company") String company,
            @Param("lastPostId") Long lastPostId,
            Pageable pageable
    );
}
