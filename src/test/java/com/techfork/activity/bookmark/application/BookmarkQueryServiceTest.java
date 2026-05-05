package com.techfork.activity.bookmark.application;

import com.techfork.activity.bookmark.presentation.BookmarkConverter;
import com.techfork.activity.bookmark.application.BookmarkDto;
import com.techfork.activity.bookmark.presentation.BookmarkListResponse;
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
    private BookmarkConverter bookmarkConverter;

    @Mock
    private CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    @InjectMocks
    private BookmarkQueryService bookmarkQueryService;

    private User mockUser;
    private List<BookmarkDto> mockBookmarksFirstPage;
    private List<BookmarkDto> mockBookmarksSecondPage;

    @BeforeEach
    void setUp() {
        lenient().when(thumbnailOptimizer.optimize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        mockUser = mock(User.class);

        mockBookmarksFirstPage = Arrays.asList(
                BookmarkDto.builder()
                        .bookmarkId(3L)
                        .postId(103L)
                        .title("게시글3")
                        .shortSummary("요약3")
                        .url("https://test.com/3")
                        .companyName("회사A")
                        .logoUrl("logo.png")
                        .publishedAt(LocalDateTime.now())
                        .thumbnailUrl("thumb3.png")
                        .viewCount(100L)
                        .keywords(List.of())
                        .isBookmarked(true)
                        .build(),
                BookmarkDto.builder()
                        .bookmarkId(2L)
                        .postId(102L)
                        .title("게시글2")
                        .shortSummary("요약2")
                        .url("https://test.com/2")
                        .companyName("회사B")
                        .logoUrl("logo.png")
                        .publishedAt(LocalDateTime.now())
                        .thumbnailUrl("thumb2.png")
                        .viewCount(200L)
                        .keywords(List.of())
                        .isBookmarked(true)
                        .build(),
                BookmarkDto.builder()
                        .bookmarkId(1L)
                        .postId(101L)
                        .title("게시글1")
                        .shortSummary("요약1")
                        .url("https://test.com/1")
                        .companyName("회사C")
                        .logoUrl("logo.png")
                        .publishedAt(LocalDateTime.now())
                        .thumbnailUrl("thumb1.png")
                        .viewCount(300L)
                        .keywords(List.of())
                        .isBookmarked(true)
                        .build()
        );

        mockBookmarksSecondPage = Arrays.asList(
                BookmarkDto.builder()
                        .bookmarkId(9L)
                        .postId(109L)
                        .title("게시글9")
                        .shortSummary("요약9")
                        .url("https://test.com/9")
                        .companyName("회사A")
                        .logoUrl("logo.png")
                        .publishedAt(LocalDateTime.now())
                        .thumbnailUrl("thumb9.png")
                        .viewCount(150L)
                        .keywords(List.of())
                        .isBookmarked(true)
                        .build(),
                BookmarkDto.builder()
                        .bookmarkId(8L)
                        .postId(108L)
                        .title("게시글8")
                        .shortSummary("요약8")
                        .url("https://test.com/8")
                        .companyName("회사B")
                        .logoUrl("logo.png")
                        .publishedAt(LocalDateTime.now())
                        .thumbnailUrl("thumb8.png")
                        .viewCount(250L)
                        .keywords(List.of())
                        .isBookmarked(true)
                        .build()
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
                        .willReturn(mockBookmarksFirstPage);
                given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());

                BookmarkListResponse expectedResponse = new BookmarkListResponse(mockBookmarksFirstPage, 2L, true);
                given(bookmarkConverter.toBookmarkListResponse(any(), eq(size))).willReturn(expectedResponse);

                BookmarkListResponse response = bookmarkQueryService.getBookmarks(query);

                assertThat(response).isNotNull();
                assertThat(response.bookmarks()).hasSize(3);
                assertThat(response.lastBookmarkId()).isEqualTo(2L);
                assertThat(response.hasNext()).isTrue();

                verify(userRepository, times(1)).findById(userId);
                verify(bookmarkRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
                verify(bookmarkConverter, times(1)).toBookmarkListResponse(mockBookmarksFirstPage, size);
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
                        .willReturn(mockBookmarksSecondPage);
                given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());

                BookmarkListResponse expectedResponse = new BookmarkListResponse(mockBookmarksSecondPage, null, false);
                given(bookmarkConverter.toBookmarkListResponse(any(), eq(size))).willReturn(expectedResponse);

                BookmarkListResponse response = bookmarkQueryService.getBookmarks(query);

                assertThat(response).isNotNull();
                assertThat(response.bookmarks()).hasSize(2);
                assertThat(response.lastBookmarkId()).isNull();
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

                List<BookmarkDto> emptyBookmarks = List.of();
                given(bookmarkRepository.findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class)))
                        .willReturn(emptyBookmarks);

                BookmarkListResponse expectedResponse = new BookmarkListResponse(emptyBookmarks, null, false);
                given(bookmarkConverter.toBookmarkListResponse(emptyBookmarks, size)).willReturn(expectedResponse);

                BookmarkListResponse response = bookmarkQueryService.getBookmarks(query);

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
                        .willReturn(mockBookmarksFirstPage);

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

                List<BookmarkDto> expectedBookmarksWithKeywords = Arrays.asList(
                        BookmarkDto.builder()
                                .bookmarkId(3L)
                                .postId(103L)
                                .title("게시글3")
                                .shortSummary("요약3")
                                .url("https://test.com/3")
                                .companyName("회사A")
                                .logoUrl("logo.png")
                                .publishedAt(mockBookmarksFirstPage.get(0).publishedAt())
                                .thumbnailUrl("thumb3.png")
                                .viewCount(100L)
                                .keywords(List.of("Java", "Spring"))
                                .isBookmarked(true)
                                .build(),
                        BookmarkDto.builder()
                                .bookmarkId(2L)
                                .postId(102L)
                                .title("게시글2")
                                .shortSummary("요약2")
                                .url("https://test.com/2")
                                .companyName("회사B")
                                .logoUrl("logo.png")
                                .publishedAt(mockBookmarksFirstPage.get(1).publishedAt())
                                .thumbnailUrl("thumb2.png")
                                .viewCount(200L)
                                .keywords(List.of("Kotlin"))
                                .isBookmarked(true)
                                .build(),
                        BookmarkDto.builder()
                                .bookmarkId(1L)
                                .postId(101L)
                                .title("게시글1")
                                .shortSummary("요약1")
                                .url("https://test.com/1")
                                .companyName("회사C")
                                .logoUrl("logo.png")
                                .publishedAt(mockBookmarksFirstPage.get(2).publishedAt())
                                .thumbnailUrl("thumb1.png")
                                .viewCount(300L)
                                .keywords(List.of())
                                .isBookmarked(true)
                                .build()
                );

                BookmarkListResponse expectedResponse = new BookmarkListResponse(expectedBookmarksWithKeywords, 2L, true);
                given(bookmarkConverter.toBookmarkListResponse(any(), eq(size))).willReturn(expectedResponse);

                BookmarkListResponse response = bookmarkQueryService.getBookmarks(query);

                assertThat(response).isNotNull();
                assertThat(response.bookmarks()).hasSize(3);
                assertThat(response.bookmarks().get(0).keywords()).containsExactlyInAnyOrder("Java", "Spring");
                assertThat(response.bookmarks().get(1).keywords()).containsExactly("Kotlin");
                assertThat(response.bookmarks().get(2).keywords()).isEmpty();

                verify(userRepository, times(1)).findById(userId);
                verify(bookmarkRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
                verify(postKeywordRepository, times(1)).findByPostIdIn(any());
                verify(bookmarkConverter, times(1)).toBookmarkListResponse(any(), eq(size));
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
