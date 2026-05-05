package com.techfork.domain.activity.bookmark.repository;

import com.techfork.domain.activity.bookmark.dto.BookmarkDto;
import com.techfork.domain.activity.bookmark.entity.Bookmark;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.useraccount.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("""
            SELECT new com.techfork.domain.activity.bookmark.dto.BookmarkDto(
                b.id, p.id, p.title, p.shortSummary, p.url, t.companyName, t.logoUrl,
                            p.publishedAt, p.thumbnailUrl, p.viewCount, null, true
            )
            FROM Bookmark b
            JOIN b.post p
            JOIN p.techBlog t
            WHERE b.user = :user
            AND (:lastBookmarkId IS NULL OR b.id < :lastBookmarkId)
            ORDER BY b.id DESC
            """)
    List<BookmarkDto> findBookmarksWithCursor(
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
