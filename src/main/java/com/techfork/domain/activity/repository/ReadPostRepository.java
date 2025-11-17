package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReadPostRepository extends JpaRepository<ReadPost, Long> {

    boolean existsByUserAndPost(User user, Post post);

    @Query("SELECT rp FROM ReadPost rp JOIN FETCH rp.post WHERE rp.user = :user ORDER BY rp.readAt DESC")
    List<ReadPost> findRecentReadPostsByUser(@Param("user") User user, Pageable pageable);
}
