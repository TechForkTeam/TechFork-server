package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByUrl(String url);

    List<Post> findBySummaryIsNull();
}
