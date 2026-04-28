package com.techfork.domain.user.repository;

import com.techfork.domain.user.document.PersonalizationProfileDocument;
import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PersonalizationProfileDocumentRepository extends ElasticsearchRepository<PersonalizationProfileDocument, String> {
    Optional<PersonalizationProfileDocument> findByUserId(Long id);
}