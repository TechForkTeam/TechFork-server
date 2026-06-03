package com.techfork.activity.readpost.application.query.lookup;

import java.util.List;

public record ReadPostLookupItem(
        String title,
        List<String> keywords,
        Integer readDurationSeconds
) {
}
