package com.techfork.activity.bookmark.application.command;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.bookmark.domain.BookmarkErrorCode;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.domain.exception.PostErrorCode;
import com.techfork.post.application.query.lookup.PostLookupService;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.global.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkCommandServiceTest {

    @Mock
    private PostLookupService postLookupService;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkCommandService bookmarkCommandService;

    @Nested
    @DisplayName("북마크 추가")
    class AddBookmark {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("북마크 추가 성공")
            void addBookmark_Success() {
                Long userId = 1L;
                Long postId = 100L;
                AddBookmarkCommand command = new AddBookmarkCommand(userId, postId);

                User mockUser = mock(User.class);
                TechBlog mockTechBlog = TechBlog.builder()
                        .companyName("테스트회사")
                        .blogUrl("https://test.com")
                        .rssUrl("https://test.com/rss")
                        .build();

                Post mockPost = Post.builder()
                        .title("테스트 제목")
                        .fullContent("내용")
                        .plainContent("내용")
                        .company("테스트회사")
                        .url("https://test.com/post/1")
                        .publishedAt(LocalDateTime.now())
                        .crawledAt(LocalDateTime.now())
                        .techBlog(mockTechBlog)
                        .build();

                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId)).willReturn(mockPost);
                given(bookmarkRepository.existsByUserAndPost(mockUser, mockPost)).willReturn(false);
                given(bookmarkRepository.save(any(Bookmark.class))).willReturn(mock(Bookmark.class));

                LocalDateTime beforeInvocation = LocalDateTime.now();

                bookmarkCommandService.addBookmark(command);

                LocalDateTime afterInvocation = LocalDateTime.now();

                ArgumentCaptor<Bookmark> bookmarkCaptor = ArgumentCaptor.forClass(Bookmark.class);

                verify(userLookupService, times(1)).getUserOrThrow(userId);
                verify(postLookupService, times(1)).getPostOrThrow(postId);
                verify(bookmarkRepository, times(1)).existsByUserAndPost(mockUser, mockPost);
                verify(bookmarkRepository, times(1)).save(bookmarkCaptor.capture());

                Bookmark savedBookmark = bookmarkCaptor.getValue();
                assertThat(savedBookmark.getUser()).isSameAs(mockUser);
                assertThat(savedBookmark.getPost()).isSameAs(mockPost);
                assertThat(savedBookmark.getBookmarkedAt())
                        .isNotNull()
                        .isBetween(beforeInvocation, afterInvocation);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("이미 북마크한 게시글")
            void addBookmark_Fail_AlreadyExists() {
                Long userId = 1L;
                Long postId = 100L;
                AddBookmarkCommand command = new AddBookmarkCommand(userId, postId);

                User mockUser = mock(User.class);
                Post mockPost = mock(Post.class);

                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId)).willReturn(mockPost);
                given(bookmarkRepository.existsByUserAndPost(mockUser, mockPost)).willReturn(true);

                assertThatThrownBy(() -> bookmarkCommandService.addBookmark(command))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", BookmarkErrorCode.BOOKMARK_ALREADY_EXISTS);

                verify(bookmarkRepository, never()).save(any());
            }

            @Test
            @DisplayName("존재하지 않는 게시글")
            void addBookmark_Fail_PostNotFound() {
                Long userId = 1L;
                Long postId = 999L;
                AddBookmarkCommand command = new AddBookmarkCommand(userId, postId);

                User mockUser = mock(User.class);
                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId))
                        .willThrow(new GeneralException(PostErrorCode.POST_NOT_FOUND));

                assertThatThrownBy(() -> bookmarkCommandService.addBookmark(command))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", PostErrorCode.POST_NOT_FOUND);

                verify(bookmarkRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("북마크 삭제")
    class DeleteBookmark {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("북마크 삭제 성공")
            void deleteBookmark_Success() {
                Long userId = 1L;
                Long postId = 100L;
                DeleteBookmarkCommand command = new DeleteBookmarkCommand(userId, postId);

                User mockUser = mock(User.class);
                Post mockPost = mock(Post.class);
                Bookmark mockBookmark = mock(Bookmark.class);

                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId)).willReturn(mockPost);
                given(bookmarkRepository.findByUserAndPost(mockUser, mockPost)).willReturn(Optional.of(mockBookmark));

                bookmarkCommandService.deleteBookmark(command);

                verify(userLookupService, times(1)).getUserOrThrow(userId);
                verify(postLookupService, times(1)).getPostOrThrow(postId);
                verify(bookmarkRepository, times(1)).findByUserAndPost(mockUser, mockPost);
                verify(bookmarkRepository, times(1)).delete(mockBookmark);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("북마크가 존재하지 않음")
            void deleteBookmark_Fail_NotFound() {
                Long userId = 1L;
                Long postId = 100L;
                DeleteBookmarkCommand command = new DeleteBookmarkCommand(userId, postId);

                User mockUser = mock(User.class);
                Post mockPost = mock(Post.class);

                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId)).willReturn(mockPost);
                given(bookmarkRepository.findByUserAndPost(mockUser, mockPost)).willReturn(Optional.empty());

                assertThatThrownBy(() -> bookmarkCommandService.deleteBookmark(command))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", BookmarkErrorCode.BOOKMARK_NOT_FOUND);

                verify(bookmarkRepository, never()).delete(any());
            }

            @Test
            @DisplayName("존재하지 않는 사용자")
            void deleteBookmark_Fail_UserNotFound() {
                Long userId = 999L;
                Long postId = 100L;
                DeleteBookmarkCommand command = new DeleteBookmarkCommand(userId, postId);

                given(userLookupService.getUserOrThrow(userId))
                        .willThrow(new GeneralException(UserErrorCode.USER_NOT_FOUND));

                assertThatThrownBy(() -> bookmarkCommandService.deleteBookmark(command))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

                verify(bookmarkRepository, never()).delete(any());
            }
        }
    }
}
