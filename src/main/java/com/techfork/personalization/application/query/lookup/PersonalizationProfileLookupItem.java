package com.techfork.personalization.application.query.lookup;

import java.util.List;

public record PersonalizationProfileLookupItem(
        float[] profileVector,
        List<String> keyKeywords
) {
    public PersonalizationProfileLookupItem {
        profileVector = profileVector == null ? null : profileVector.clone();
        keyKeywords = keyKeywords == null ? List.of() : List.copyOf(keyKeywords);
    }

    @Override
    public float[] profileVector() {
        return profileVector == null ? null : profileVector.clone();
    }
}
