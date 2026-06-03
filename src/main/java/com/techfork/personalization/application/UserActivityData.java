package com.techfork.personalization.application;

import java.util.List;

public record UserActivityData(
        List<String> interests,
        List<PostActivityData> readPostData,
        List<PostActivityData> bookmarkedPostData,
        List<String> searchQueries
) {
}
