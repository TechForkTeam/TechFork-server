package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.PostMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostMetadataRepository extends JpaRepository<PostMetadata, Long> {

    boolean existsByPostId(Long postId);
}
