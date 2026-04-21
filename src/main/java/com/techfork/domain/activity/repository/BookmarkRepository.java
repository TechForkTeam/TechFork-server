package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.dto.BookmarkDto;
import com.techfork.domain.activity.entity.Bookmark;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("""
            SELECT new com.techfork.domain.activity.dto.BookmarkDto(
                s.id, p.id, p.title, p.shortSummary, p.url, t.companyName, t.logoUrl,
                            p.publishedAt, p.thumbnailUrl, p.viewCount, null, true
            )
            FROM Bookmark s
            JOIN s.post p
            JOIN p.techBlog t
            WHERE s.user = :user
            AND (:lastBookmarkId IS NULL OR s.id < :lastBookmarkId)
            ORDER BY s.id DESC
            """)
    List<BookmarkDto> findBookmarksWithCursor(
            @Param("user") User user,
            @Param("lastBookmarkId") Long lastBookmarkId,
            Pageable pageable
    );

    boolean existsByUserAndPost(User user, Post post);

    Optional<Bookmark> findByUserAndPost(User user, Post post);

    @Query("SELECT sp FROM Bookmark sp JOIN FETCH sp.post WHERE sp.user.id = :userId ORDER BY sp.bookmarkedAt DESC")
    List<Bookmark> findRecentBookmarksByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT sp.post.id
            FROM Bookmark sp
            WHERE sp.user.id = :userId
            AND sp.post.id IN :postIds
            """)
    List<Long> findBookmarkedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
