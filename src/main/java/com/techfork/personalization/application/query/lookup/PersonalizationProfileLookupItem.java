package com.techfork.personalization.application.query.lookup;

import java.util.List;

public record PersonalizationProfileLookupItem(
        float[] profileVector,
        List<String> keyKeywords
) {
}
