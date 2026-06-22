package com.techfork.personalization.fixture;

import com.techfork.personalization.infrastructure.PersonalizationProfileDocument;

import java.util.List;

public final class PersonalizationProfileDocumentFixture {

    private PersonalizationProfileDocumentFixture() {
    }

    public static PersonalizationProfileDocument personalizationProfileDocument(
            Long userId,
            String profileText,
            float[] profileVector,
            List<String> interests,
            List<String> keyKeywords
    ) {
        return PersonalizationProfileDocument.create(userId, profileText, profileVector, interests, keyKeywords);
    }
}
