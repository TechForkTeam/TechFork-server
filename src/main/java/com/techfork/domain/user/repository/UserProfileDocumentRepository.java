package com.techfork.domain.user.repository;

import com.techfork.domain.user.document.UserProfileDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserProfileDocumentRepository extends ElasticsearchRepository<UserProfileDocument, String> {
}
