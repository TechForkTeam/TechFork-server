package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByUrl(String url);
}
