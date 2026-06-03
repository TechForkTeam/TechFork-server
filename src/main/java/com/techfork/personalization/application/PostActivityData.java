package com.techfork.personalization.application;

import java.util.List;

public record PostActivityData(
        String title,
        List<String> keywords,
        String readingEngagement
) {
}
