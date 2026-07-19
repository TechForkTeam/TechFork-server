package com.techfork.personalization.application.query.lookup;

import com.techfork.personalization.infrastructure.PersonalizationProfileDocumentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizationProfileLookupService {

    private final PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    public Optional<PersonalizationProfileLookupItem> findByUserId(Long userId) {
        return personalizationProfileDocumentRepository.findByUserId(userId)
                .map(profile -> new PersonalizationProfileLookupItem(
                        profile.getProfileVector(),
                        profile.getKeyKeywords()
                ));
    }
}
