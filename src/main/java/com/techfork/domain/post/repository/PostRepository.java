package com.techfork.domain.post.repository;

import com.techfork.domain.post.dto.CompanyDto;
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
import java.util.Set;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p.url FROM Post p WHERE p.url IN :urls")
    Set<String> findExistingUrls(@Param("urls") List<String> urls);

    @Query("""
            SELECT DISTINCT p FROM Post p 
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
            SELECT new com.techfork.domain.post.dto.CompanyDto(
                p.company,
                (COUNT(CASE WHEN p.publishedAt >= CURRENT_DATE THEN 1 END) > 0),
                MAX(p.logoUrl)
            )
            FROM Post p
            GROUP BY p.company
            ORDER BY MAX(p.publishedAt) DESC
            """)
    List<CompanyDto> findCompaniesWithDetails();

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostInfoDto(
            p.id, p.title, p.company, p.url, p.logoUrl, p.publishedAt, p.viewCount, null)
            FROM Post p
            WHERE (:company IS NULL OR p.company = :company)
            AND (:lastPostId IS NULL OR p.id < :lastPostId)
            ORDER BY p.publishedAt DESC
            """)
    List<PostInfoDto> findByCompanyWithCursor(
            @Param("company") String company,
            @Param("lastPostId") Long lastPostId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostInfoDto(
            p.id, p.title, p.company, p.url, p.logoUrl, p.publishedAt, p.viewCount, null)
            FROM Post p
            WHERE :lastPostId IS NULL OR p.id < :lastPostId
            ORDER BY p.publishedAt DESC
            """)
    List<PostInfoDto> findRecentPostsWithCursor(
            @Param("lastPostId") Long lastPostId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostInfoDto(
            p.id, p.title, p.company, p.url, p.logoUrl, p.publishedAt, p.viewCount, null)
            FROM Post p
            WHERE :lastPostId IS NULL OR p.id < :lastPostId
            ORDER BY p.viewCount DESC, p.publishedAt DESC
            """)
    List<PostInfoDto> findPopularPostsWithCursor(
            @Param("lastPostId") Long lastPostId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.techfork.domain.post.dto.PostDetailDto(
            p.id, p.title, p.summary, p.company, p.url, p.logoUrl, p.publishedAt, p.viewCount, null)
            FROM Post p
            WHERE p.id = :id
            """)
    Optional<PostDetailDto> findByIdWithTechBlog(@Param("id") Long id);
}
