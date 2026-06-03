package com.techfork.personalization.application.event;

import java.util.List;

public record PersonalizedProfileGeneratedEvent(
        Long userId,
        float[] profileVector,
        List<String> keyKeywords
) {
    public PersonalizedProfileGeneratedEvent {
        profileVector = profileVector == null ? null : profileVector.clone();
        keyKeywords = keyKeywords == null ? List.of() : List.copyOf(keyKeywords);
    }

    @Override
    public float[] profileVector() {
        return profileVector == null ? null : profileVector.clone();
    }
}
