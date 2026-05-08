package com.techfork.activity.readpost.infrastructure;

import com.techfork.activity.readpost.domain.ReadPost;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadPostRepository extends JpaRepository<ReadPost, Long> {

    @Query("""
            SELECT rp FROM ReadPost rp
            JOIN FETCH rp.post
            WHERE rp.user.id = :userId
            AND (rp.readDurationSeconds IS NULL OR rp.readDurationSeconds > 10)
            ORDER BY rp.readAt DESC
            """)
    List<ReadPost> findRecentReadPostsByUserIdWithMinDuration(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT new com.techfork.activity.readpost.infrastructure.ReadPostQueryRow(
                rp.id, p.id, p.title, p.shortSummary, p.url, t.companyName, t.logoUrl,
                p.publishedAt, p.thumbnailUrl, p.viewCount, rp.readAt
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
    List<ReadPostQueryRow> findReadPostsWithCursor(
            @Param("userId") Long userId,
            @Param("lastReadPostId") Long lastReadPostId,
            Pageable pageable
    );
}
