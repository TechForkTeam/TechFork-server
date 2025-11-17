package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.dto.BookmarkDto;
import com.techfork.domain.activity.entity.ScrabPost;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScrabPostRepository extends JpaRepository<ScrabPost, Long> {

    @Query("""
            SELECT new com.techfork.domain.activity.dto.BookmarkDto(
                s.id, p.id, p.title, p.url, t.companyName, t.logoUrl, p.publishedAt
            )
            FROM ScrabPost s
            JOIN s.post p
            JOIN p.techBlog t
            WHERE s.user = :user
            AND (:lastBookmarkId IS NULL OR s.id < :lastBookmarkId)
            ORDER BY s.scrappedAt DESC, s.id DESC
            """)
    List<BookmarkDto> findBookmarksWithCursor(
            @Param("user") User user,
            @Param("lastBookmarkId") Long lastBookmarkId,
            Pageable pageable
    );

    boolean existsByUserAndPost(User user, Post post);

    Optional<ScrabPost> findByUserAndPost(User user, Post post);
}
