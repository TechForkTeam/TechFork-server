package com.techfork.activity.bookmark.infrastructure;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.post.domain.Post;
import com.techfork.domain.useraccount.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("""
            SELECT new com.techfork.activity.bookmark.infrastructure.BookmarkQueryRow(
                b.id, p.id, p.title, p.shortSummary, p.url, t.companyName, t.logoUrl,
                            p.publishedAt, p.thumbnailUrl, p.viewCount
            )
            FROM Bookmark b
            JOIN b.post p
            JOIN p.techBlog t
            WHERE b.user = :user
            AND (:lastBookmarkId IS NULL OR b.id < :lastBookmarkId)
            ORDER BY b.id DESC
            """)
    List<BookmarkQueryRow> findBookmarksWithCursor(
            @Param("user") User user,
            @Param("lastBookmarkId") Long lastBookmarkId,
            Pageable pageable
    );

    boolean existsByUserAndPost(User user, Post post);

    Optional<Bookmark> findByUserAndPost(User user, Post post);

    @Query("SELECT b FROM Bookmark b JOIN FETCH b.post WHERE b.user.id = :userId ORDER BY b.bookmarkedAt DESC")
    List<Bookmark> findRecentBookmarksByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT b.post.id
            FROM Bookmark b
            WHERE b.user.id = :userId
            AND b.post.id IN :postIds
            """)
    List<Long> findBookmarkedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
