package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.dto.ReadPostDto;
import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.useraccount.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReadPostRepository extends JpaRepository<ReadPost, Long> {

    boolean existsByUserAndPost(User user, Post post);

    @Query("""
            SELECT rp FROM ReadPost rp
            JOIN FETCH rp.post
            WHERE rp.user.id = :userId
            AND (rp.readDurationSeconds IS NULL OR rp.readDurationSeconds > 10)
            ORDER BY rp.readAt DESC
            """)
    List<ReadPost> findRecentReadPostsByUserIdWithMinDuration(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT new com.techfork.domain.activity.dto.ReadPostDto(
                rp.id, p.id, p.title, p.shortSummary, p.url, t.companyName, t.logoUrl,
                p.publishedAt, p.thumbnailUrl, p.viewCount, null, null, rp.readAt
            )
            FROM ReadPost rp
            JOIN rp.post p
            JOIN p.techBlog t
            WHERE rp.user.id = :userId
            AND rp.id IN (
                SELECT MAX(rp2.id)
                FROM ReadPost rp2
                WHERE rp2.user.id = :userId
                GROUP BY rp2.post.id
            )
            AND (:lastReadPostId IS NULL OR rp.id < :lastReadPostId)
            ORDER BY rp.id DESC
            """)
    List<ReadPostDto> findReadPostsWithCursor(
            @Param("userId") Long userId,
            @Param("lastReadPostId") Long lastReadPostId,
            Pageable pageable
    );
}
