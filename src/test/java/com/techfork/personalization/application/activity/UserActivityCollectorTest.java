package com.techfork.personalization.application.activity;

import com.techfork.activity.bookmark.application.query.lookup.BookmarkLookupService;
import com.techfork.activity.bookmark.application.query.lookup.BookmarkPostLookupItem;
import com.techfork.activity.readhistory.application.query.lookup.SearchHistoryLookupService;
import com.techfork.activity.readpost.application.query.lookup.ReadPostLookupItem;
import com.techfork.activity.readpost.application.query.lookup.ReadPostLookupService;
import com.techfork.useraccount.application.query.lookup.UserInterestLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserActivityCollectorTest {

    @Mock
    private UserInterestLookupService userInterestLookupService;

    @Mock
    private ReadPostLookupService readPostLookupService;

    @Mock
    private BookmarkLookupService bookmarkLookupService;

    @Mock
    private SearchHistoryLookupService searchHistoryLookupService;

    @InjectMocks
    private UserActivityCollector userActivityCollector;

    @Test
    @DisplayName("컨텍스트 lookup 결과를 개인화 활동 데이터로 조합한다")
    void collect_ComposesUserActivityDataFromLookupServices() {
        Long userId = 1L;
        given(userInterestLookupService.getInterestKeywordDisplayNames(userId))
                .willReturn(List.of("Java", "Spring"));
        given(readPostLookupService.getRecentReadPostActivities(userId, 20))
                .willReturn(List.of(
                        readPostLookupItem("null 포스트", List.of("Java"), null),
                        readPostLookupItem("30초 포스트", List.of("Spring"), 30),
                        readPostLookupItem("90초 포스트", List.of("JPA"), 90),
                        readPostLookupItem("300초 포스트", List.of("Kafka"), 300),
                        readPostLookupItem("600초 포스트", List.of("Docker"), 600),
                        readPostLookupItem("601초 포스트", List.of("Elastic"), 601)
                ));
        given(bookmarkLookupService.getRecentBookmarkPostActivities(userId, 20))
                .willReturn(List.of(bookmarkPostLookupItem("북마크 포스트", List.of("Kubernetes", "Helm"))));
        given(searchHistoryLookupService.getRecentSearchQueries(userId, 30))
                .willReturn(List.of("Spring Batch", "Elasticsearch vector"));

        UserActivityData result = userActivityCollector.collect(userId);

        assertThat(result.interests()).containsExactly("Java", "Spring");
        assertThat(result.readPostData()).containsExactly(
                postActivityData("null 포스트", List.of("Java"), "읽음"),
                postActivityData("30초 포스트", List.of("Spring"), "가볍게 훑어봄"),
                postActivityData("90초 포스트", List.of("JPA"), "빠르게 읽음"),
                postActivityData("300초 포스트", List.of("Kafka"), "읽음"),
                postActivityData("600초 포스트", List.of("Docker"), "정독함"),
                postActivityData("601초 포스트", List.of("Elastic"), "깊게 읽음")
        );
        assertThat(result.bookmarkedPostData())
                .containsExactly(postActivityData("북마크 포스트", List.of("Kubernetes", "Helm"), null));
        assertThat(result.searchQueries()).containsExactly("Spring Batch", "Elasticsearch vector");

        verify(userInterestLookupService).getInterestKeywordDisplayNames(userId);
        verify(readPostLookupService).getRecentReadPostActivities(userId, 20);
        verify(bookmarkLookupService).getRecentBookmarkPostActivities(userId, 20);
        verify(searchHistoryLookupService).getRecentSearchQueries(userId, 30);
    }

    private ReadPostLookupItem readPostLookupItem(String title, List<String> keywords, Integer readDurationSeconds) {
        return new ReadPostLookupItem(title, keywords, readDurationSeconds);
    }

    private BookmarkPostLookupItem bookmarkPostLookupItem(String title, List<String> keywords) {
        return new BookmarkPostLookupItem(title, keywords);
    }

    private PostActivityData postActivityData(String title, List<String> keywords, String readingEngagement) {
        return new PostActivityData(title, keywords, readingEngagement);
    }
}
