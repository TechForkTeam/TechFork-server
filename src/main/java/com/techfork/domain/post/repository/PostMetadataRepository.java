package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.PostMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostMetadataRepository extends JpaRepository<PostMetadata, Long> {

    boolean existsByPostId(Long postId);

    /**
     * 기술 스택 정보가 모두 비어있는 메타데이터 조회
     * (Rate Limit으로 실패한 케이스)
     */
    @Query("SELECT pm FROM PostMetadata pm WHERE " +
            "(pm.languages IS NULL OR pm.languages = '') AND " +
            "(pm.frameworks IS NULL OR pm.frameworks = '') AND " +
            "(pm.tools IS NULL OR pm.tools = '') AND " +
            "(pm.mainTopics IS NULL OR pm.mainTopics = '')")
    List<PostMetadata> findEmptyMetadata();
}
