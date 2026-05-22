package com.techfork.post.infrastructure;

import com.techfork.post.domain.projection.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostDocumentRepository extends ElasticsearchRepository<PostDocument, String> {

    Optional<PostDocument> findByPostId(Long postId);
}
