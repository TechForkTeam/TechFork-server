package com.techfork.personalization.application.generation;

import java.util.List;

public record PersonalizationProfileAnalysis(
        String profileText,
        List<String> keyKeywords
) {
}
