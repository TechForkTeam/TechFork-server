package com.techfork.domain.post.repository;

import com.techfork.domain.post.dto.PostDetailDto;
import com.techfork.domain.post.dto.PostInfoDto;
import com.techfork.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByUrl(String url);

    @Query("""
            SELECT p FROM Post p 
            LEFT JOIN FETCH p.keywords
            WHERE p.summary IS NULL OR p.summary = ''
            """)
    List<Post> findWithKeywordsBySummaryIsNull();

    Page<Post> findBySummaryIsNotNullAndEmbeddedAtIsNull(Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.embeddedAt = :embeddedAt WHERE p.id IN :ids")
    void bulkUpdateEmbeddedAt(@Param("ids") List<Long> ids, @Param("embeddedAt") LocalDateTime embeddedAt);

    @Query("SELECT DISTINCT p.company FROM Post p ORDER BY p.company")
    List<String> findDistinctCompanies();

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostInfoDto(
            p.id, p.title, t.companyName, p.url, t.logoUrl, p.publishedAt)
            FROM Post p
            JOIN TechBlog t on p.techBlog.id = t.id
            WHERE (:company IS NULL OR p.company = :company)
            AND (:lastPostId IS NULL OR p.id < :lastPostId)
            ORDER BY p.id DESC
            """)
    List<PostInfoDto> findByCompanyWithCursor(
            @Param("company") String company,
            @Param("lastPostId") Long lastPostId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostInfoDto(
            p.id, p.title, t.companyName, p.url, t.logoUrl, p.publishedAt)
            FROM Post p
            JOIN TechBlog t on p.techBlog.id = t.id
            WHERE :lastPostId IS NULL OR p.id < :lastPostId 
            ORDER BY p.publishedAt DESC, p.id DESC
            """)
    List<PostInfoDto> findRecentPostsWithCursor(
            @Param("lastPostId") Long lastPostId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostInfoDto(
            p.id, p.title, t.companyName, p.url, t.logoUrl, p.publishedAt)
            FROM Post p
            JOIN TechBlog t on p.techBlog.id = t.id
            WHERE :lastPostId IS NULL OR p.id < :lastPostId
            ORDER BY p.crawledAt DESC, p.id DESC
            """)
    List<PostInfoDto> findPopularPostsWithCursor(
            @Param("lastPostId") Long lastPostId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostDetailDto(
            p.id, p.title, p.summary, t.companyName, p.url, t.logoUrl, p.publishedAt)
            FROM Post p
            JOIN TechBlog t on p.techBlog.id = t.id
            WHERE p.id = :id
            """)
    Optional<PostDetailDto> findByIdWithTechBlog(@Param("id") Long id);
}
