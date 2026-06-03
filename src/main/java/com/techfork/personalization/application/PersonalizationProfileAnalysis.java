package com.techfork.personalization.application;

import java.util.List;

public record PersonalizationProfileAnalysis(
        String profileText,
        List<String> keyKeywords
) {
}
