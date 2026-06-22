package com.techfork.activity.readhistory.fixture;

import com.techfork.activity.readhistory.domain.SearchHistory;
import com.techfork.useraccount.domain.User;

import java.time.LocalDateTime;

public final class SearchHistoryFixture {

    private SearchHistoryFixture() {
    }

    public static SearchHistory createSearchHistory(User user, String query, LocalDateTime searchedAt) {
        return SearchHistory.create(user, query, searchedAt);
    }
}
