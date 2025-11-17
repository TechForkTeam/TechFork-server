package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadPostRepository extends JpaRepository<ReadPost, Long> {

    boolean existsByUserAndPost(User user, Post post);
}
