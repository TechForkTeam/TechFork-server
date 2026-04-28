package com.techfork.domain.personalization.repository;

import com.techfork.domain.personalization.document.PersonalizationProfileDocument;
import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PersonalizationProfileDocumentRepository extends ElasticsearchRepository<PersonalizationProfileDocument, String> {
    Optional<PersonalizationProfileDocument> findByUserId(Long id);
}