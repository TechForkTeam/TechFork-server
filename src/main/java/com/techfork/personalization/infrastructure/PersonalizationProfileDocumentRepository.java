package com.techfork.personalization.infrastructure;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PersonalizationProfileDocumentRepository extends ElasticsearchRepository<PersonalizationProfileDocument, String> {
    Optional<PersonalizationProfileDocument> findByUserId(Long id);
}