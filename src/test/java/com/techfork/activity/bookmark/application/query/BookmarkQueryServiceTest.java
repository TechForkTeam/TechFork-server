package com.techfork.activity.bookmark.application.query;

import com.techfork.activity.bookmark.infrastructure.BookmarkQueryRow;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BookmarkQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostKeywordRepository postKeywordRepository;

    @Mock
    private CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    @InjectMocks
    private BookmarkQueryService bookmarkQueryService;

    private User mockUser;
    private List<BookmarkQueryRow> mockBookmarkRowsFirstPage;
    private List<BookmarkQueryRow> mockBookmarkRowsSecondPage;

    @BeforeEach
    void setUp() {
        lenient().when(thumbnailOptimizer.optimize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        mockUser = mock(User.class);

        mockBookmarkRowsFirstPage = Arrays.asList(
                new BookmarkQueryRow(3L, 103L, "게시글3", "요약3", "https://test.com/3", "회사A", "logo.png",
                        LocalDateTime.now(), "thumb3.png", 100L, true),
                new BookmarkQueryRow(2L, 102L, "게시글2", "요약2", "https://test.com/2", "회사B", "logo.png",
                        LocalDateTime.now(), "thumb2.png", 200L, true),
                new BookmarkQueryRow(1L, 101L, "게시글1", "요약1", "https://test.com/1", "회사C", "logo.png",
                        LocalDateTime.now(), "thumb1.png", 300L, true)
        );

        mockBookmarkRowsSecondPage = Arrays.asList(
                new BookmarkQueryRow(9L, 109L, "게시글9", "요약9", "https://test.com/9", "회사A", "logo.png",
                        LocalDateTime.now(), "thumb9.png", 150L, true),
                new BookmarkQueryRow(8L, 108L, "게시글8", "요약8", "https://test.com/8", "회사B", "logo.png",
                        LocalDateTime.now(), "thumb8.png", 250L, true)
        );
    }

    @Nested
    @DisplayName("북마크 목록 조회")
    class GetBookmarks {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("첫 페이지")
            void getBookmarks_Success_FirstPage() {
                Long userId = 1L;
                Long lastBookmarkId = null;
                int size = 20;
                GetBookmarksQuery query = new GetBookmarksQuery(userId, lastBookmarkId, size);

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(bookmarkRepository.findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class)))
                        .willReturn(mockBookmarkRowsFirstPage);
                given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());

                GetBookmarksResult response = bookmarkQueryService.getBookmarks(query);

                assertThat(response).isNotNull();
                assertThat(response.bookmarks()).hasSize(3);
                assertThat(response.lastBookmarkId()).isEqualTo(1L);
                assertThat(response.hasNext()).isFalse();
                assertThat(response.bookmarks().get(0).bookmarkId()).isEqualTo(3L);

                verify(userRepository, times(1)).findById(userId);
                verify(bookmarkRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
            }

            @Test
            @DisplayName("커서 기반 페이징")
            void getBookmarks_Success_WithCursor() {
                Long userId = 1L;
                Long lastBookmarkId = 10L;
                int size = 20;
                GetBookmarksQuery query = new GetBookmarksQuery(userId, lastBookmarkId, size);

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(bookmarkRepository.findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class)))
                        .willReturn(mockBookmarkRowsSecondPage);
                given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());

                GetBookmarksResult response = bookmarkQueryService.getBookmarks(query);

                assertThat(response).isNotNull();
                assertThat(response.bookmarks()).hasSize(2);
                assertThat(response.lastBookmarkId()).isEqualTo(8L);
                assertThat(response.hasNext()).isFalse();

                verify(bookmarkRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
            }

            @Test
            @DisplayName("빈 목록")
            void getBookmarks_Success_EmptyList() {
                Long userId = 1L;
                Long lastBookmarkId = null;
                int size = 20;
                GetBookmarksQuery query = new GetBookmarksQuery(userId, lastBookmarkId, size);

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

                List<BookmarkQueryRow> emptyBookmarks = List.of();
                given(bookmarkRepository.findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class)))
                        .willReturn(emptyBookmarks);

                GetBookmarksResult response = bookmarkQueryService.getBookmarks(query);

                assertThat(response).isNotNull();
                assertThat(response.bookmarks()).isEmpty();
                assertThat(response.lastBookmarkId()).isNull();
                assertThat(response.hasNext()).isFalse();

                verify(bookmarkRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
            }

            @Test
            @DisplayName("키워드 포함")
            void getBookmarks_Success_WithKeywords() {
                Long userId = 1L;
                Long lastBookmarkId = null;
                int size = 20;
                GetBookmarksQuery query = new GetBookmarksQuery(userId, lastBookmarkId, size);

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(bookmarkRepository.findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class)))
                        .willReturn(mockBookmarkRowsFirstPage);

                Post mockPost1 = mock(Post.class);
                Post mockPost2 = mock(Post.class);

                given(mockPost1.getId()).willReturn(103L);
                given(mockPost2.getId()).willReturn(102L);

                PostKeyword keyword1 = PostKeyword.builder()
                        .keyword("Java")
                        .post(mockPost1)
                        .build();
                PostKeyword keyword2 = PostKeyword.builder()
                        .keyword("Spring")
                        .post(mockPost1)
                        .build();
                PostKeyword keyword3 = PostKeyword.builder()
                        .keyword("Kotlin")
                        .post(mockPost2)
                        .build();

                given(postKeywordRepository.findByPostIdIn(any()))
                        .willReturn(List.of(keyword1, keyword2, keyword3));

                GetBookmarksResult response = bookmarkQueryService.getBookmarks(query);

                assertThat(response).isNotNull();
                assertThat(response.bookmarks()).hasSize(3);
                assertThat(response.bookmarks().get(0).keywords()).containsExactlyInAnyOrder("Java", "Spring");
                assertThat(response.bookmarks().get(1).keywords()).containsExactly("Kotlin");
                assertThat(response.bookmarks().get(2).keywords()).isEmpty();

                verify(userRepository, times(1)).findById(userId);
                verify(bookmarkRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
                verify(postKeywordRepository, times(1)).findByPostIdIn(any());
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 사용자")
            void getBookmarks_Fail_UserNotFound() {
                Long userId = 999L;
                Long lastBookmarkId = null;
                int size = 20;
                GetBookmarksQuery query = new GetBookmarksQuery(userId, lastBookmarkId, size);

                given(userRepository.findById(userId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> bookmarkQueryService.getBookmarks(query))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

                verify(userRepository, times(1)).findById(userId);
                verify(bookmarkRepository, never()).findBookmarksWithCursor(any(), any(), any());
            }
        }
    }
}
