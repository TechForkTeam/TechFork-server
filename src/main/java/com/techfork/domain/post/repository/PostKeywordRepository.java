package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostKeywordRepository extends JpaRepository<PostKeyword, Long> {

    List<PostKeyword> findByPost(Post post);

    void deleteByPost(Post post);
}
