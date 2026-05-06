package com.techfork.activity.readpost.application.query;

import java.util.List;

public record GetReadPostsResult(
        List<ReadPostItem> readPosts,
        Long lastReadPostId,
        boolean hasNext
) {
}
