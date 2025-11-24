package com.techfork.domain.post.repository;

import com.techfork.domain.post.document.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostDocumentRepository extends ElasticsearchRepository<PostDocument, String> {

    Optional<PostDocument> findByPostId(Long postId);
}
