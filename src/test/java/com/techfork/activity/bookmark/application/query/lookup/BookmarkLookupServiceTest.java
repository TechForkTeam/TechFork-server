package com.techfork.activity.bookmark.application.query.lookup;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.domain.PostKeyword;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkLookupServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkLookupService bookmarkLookupService;

    @Nested
    @DisplayName("북마크된 게시글 ID 조회")
    class GetBookmarkedPostIds {

        @Test
        @DisplayName("입력 postIds가 비어있으면 repository 호출 없이 빈 Set을 반환한다")
        void emptyPostIds_ReturnsEmptySet() {
            Set<Long> result = bookmarkLookupService.getBookmarkedPostIds(1L, List.of());

            assertThat(result).isEmpty();
            verify(bookmarkRepository, never()).findBookmarkedPostIds(anyLong(), any());
        }

        @Test
        @DisplayName("repository 조회 결과를 Set으로 변환해 반환한다")
        void postIdsProvided_ReturnsRepositoryResultAsSet() {
            Long userId = 1L;
            List<Long> postIds = List.of(101L, 102L, 103L);
            given(bookmarkRepository.findBookmarkedPostIds(userId, postIds)).willReturn(List.of(101L, 103L));

            Set<Long> result = bookmarkLookupService.getBookmarkedPostIds(userId, postIds);

            assertThat(result).containsExactlyInAnyOrder(101L, 103L);
        }
    }

    @Nested
    @DisplayName("최근 북마크 게시글 활동 신호 조회")
    class GetRecentBookmarkPostActivities {

        @Test
        @DisplayName("최근 북마크 게시글 제목과 키워드를 반환한다")
        void sinceDate_ReturnsBookmarkPostActivities() {
            Long userId = 1L;
            int limit = 20;
            Bookmark bookmark = bookmark("북마크 포스트", List.of("Kubernetes", "Helm"));
            given(bookmarkRepository.findRecentBookmarksByUserId(userId, PageRequest.of(0, limit)))
                    .willReturn(List.of(bookmark));

            List<BookmarkPostLookupItem> result = bookmarkLookupService.getRecentBookmarkPostActivities(userId, limit);

            assertThat(result).containsExactly(new BookmarkPostLookupItem("북마크 포스트", List.of("Kubernetes", "Helm")));
            verify(bookmarkRepository).findRecentBookmarksByUserId(userId, PageRequest.of(0, limit));
        }
    }

    private Bookmark bookmark(String title, List<String> keywords) {
        Bookmark bookmark = mock(Bookmark.class);
        Post post = mock(Post.class);
        List<PostKeyword> postKeywords = keywords.stream()
                .map(this::postKeyword)
                .toList();
        given(bookmark.getPost()).willReturn(post);
        given(post.getTitle()).willReturn(title);
        given(post.getKeywords()).willReturn(postKeywords);
        return bookmark;
    }

    private PostKeyword postKeyword(String keyword) {
        PostKeyword postKeyword = mock(PostKeyword.class);
        given(postKeyword.getKeyword()).willReturn(keyword);
        return postKeyword;
    }
}
