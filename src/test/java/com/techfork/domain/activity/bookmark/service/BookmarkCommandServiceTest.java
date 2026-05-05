package com.techfork.domain.activity.bookmark.service;

import com.techfork.domain.activity.bookmark.dto.BookmarkRequest;
import com.techfork.domain.activity.bookmark.entity.Bookmark;
import com.techfork.domain.activity.bookmark.exception.BookmarkErrorCode;
import com.techfork.domain.activity.bookmark.repository.BookmarkRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.exception.PostErrorCode;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
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
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkCommandService bookmarkCommandService;

    @Test
    @DisplayName("북마크 추가 성공")
    void addBookmark_Success() {
        Long userId = 1L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

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

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(bookmarkRepository.existsByUserAndPost(mockUser, mockPost)).willReturn(false);
        given(bookmarkRepository.save(any(Bookmark.class))).willReturn(mock(Bookmark.class));

        LocalDateTime beforeInvocation = LocalDateTime.now();

        bookmarkCommandService.addBookmark(userId, request);

        LocalDateTime afterInvocation = LocalDateTime.now();

        ArgumentCaptor<Bookmark> bookmarkCaptor = ArgumentCaptor.forClass(Bookmark.class);

        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, times(1)).findById(postId);
        verify(bookmarkRepository, times(1)).existsByUserAndPost(mockUser, mockPost);
        verify(bookmarkRepository, times(1)).save(bookmarkCaptor.capture());

        Bookmark savedBookmark = bookmarkCaptor.getValue();
        assertThat(savedBookmark.getUser()).isSameAs(mockUser);
        assertThat(savedBookmark.getPost()).isSameAs(mockPost);
        assertThat(savedBookmark.getBookmarkedAt())
                .isNotNull()
                .isBetween(beforeInvocation, afterInvocation);
    }

    @Test
    @DisplayName("북마크 추가 실패 - 이미 북마크한 게시글")
    void addBookmark_Fail_AlreadyExists() {
        Long userId = 1L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

        User mockUser = mock(User.class);
        Post mockPost = mock(Post.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(bookmarkRepository.existsByUserAndPost(mockUser, mockPost)).willReturn(true);

        assertThatThrownBy(() -> bookmarkCommandService.addBookmark(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", BookmarkErrorCode.BOOKMARK_ALREADY_EXISTS);

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("북마크 추가 실패 - 존재하지 않는 게시글")
    void addBookmark_Fail_PostNotFound() {
        Long userId = 1L;
        Long postId = 999L;
        BookmarkRequest request = new BookmarkRequest(postId);

        User mockUser = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkCommandService.addBookmark(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", PostErrorCode.POST_NOT_FOUND);

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("북마크 삭제 성공")
    void deleteBookmark_Success() {
        Long userId = 1L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

        User mockUser = mock(User.class);
        Post mockPost = mock(Post.class);
        Bookmark mockBookmark = mock(Bookmark.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(bookmarkRepository.findByUserAndPost(mockUser, mockPost)).willReturn(Optional.of(mockBookmark));

        bookmarkCommandService.deleteBookmark(userId, request);

        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, times(1)).findById(postId);
        verify(bookmarkRepository, times(1)).findByUserAndPost(mockUser, mockPost);
        verify(bookmarkRepository, times(1)).delete(mockBookmark);
    }

    @Test
    @DisplayName("북마크 삭제 실패 - 북마크가 존재하지 않음")
    void deleteBookmark_Fail_NotFound() {
        Long userId = 1L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

        User mockUser = mock(User.class);
        Post mockPost = mock(Post.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(bookmarkRepository.findByUserAndPost(mockUser, mockPost)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkCommandService.deleteBookmark(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", BookmarkErrorCode.BOOKMARK_NOT_FOUND);

        verify(bookmarkRepository, never()).delete(any());
    }

    @Test
    @DisplayName("북마크 삭제 실패 - 존재하지 않는 사용자")
    void deleteBookmark_Fail_UserNotFound() {
        Long userId = 999L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkCommandService.deleteBookmark(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(bookmarkRepository, never()).delete(any());
    }
}
