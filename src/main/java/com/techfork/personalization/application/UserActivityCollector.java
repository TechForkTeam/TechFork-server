package com.techfork.personalization.application;

import com.techfork.activity.bookmark.application.query.lookup.BookmarkLookupService;
import com.techfork.activity.readhistory.application.query.lookup.SearchHistoryLookupService;
import com.techfork.activity.readpost.application.query.lookup.ReadPostLookupService;
import com.techfork.useraccount.application.query.lookup.UserInterestLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserActivityCollector {

    private static final int RECENT_READ_POST_LIMIT = 20;
    private static final int RECENT_BOOKMARK_LIMIT = 20;
    private static final int RECENT_SEARCH_QUERY_LIMIT = 30;

    private final UserInterestLookupService userInterestLookupService;
    private final ReadPostLookupService readPostLookupService;
    private final BookmarkLookupService bookmarkLookupService;
    private final SearchHistoryLookupService searchHistoryLookupService;

    public UserActivityData collect(Long userId) {
        return new UserActivityData(
                userInterestLookupService.getInterestKeywordDisplayNames(userId),
                readPostLookupService.getRecentReadPostActivities(userId, RECENT_READ_POST_LIMIT)
                        .stream()
                        .map(item -> new PostActivityData(
                                item.title(),
                                item.keywords(),
                                convertReadingDurationToNaturalLanguage(item.readDurationSeconds())
                        ))
                        .toList(),
                bookmarkLookupService.getRecentBookmarkPostActivities(userId, RECENT_BOOKMARK_LIMIT)
                        .stream()
                        .map(item -> new PostActivityData(
                                item.title(),
                                item.keywords(),
                                null
                        ))
                        .toList(),
                searchHistoryLookupService.getRecentSearchQueries(userId, RECENT_SEARCH_QUERY_LIMIT)
        );
    }

    private String convertReadingDurationToNaturalLanguage(Integer durationSeconds) {
        if (durationSeconds == null) {
            return "읽음";
        }

        if (durationSeconds <= 30) {
            return "가볍게 훑어봄";
        } else if (durationSeconds <= 90) {
            return "빠르게 읽음";
        } else if (durationSeconds <= 300) {
            return "읽음";
        } else if (durationSeconds <= 600) {
            return "정독함";
        } else {
            return "깊게 읽음";
        }
    }
}
