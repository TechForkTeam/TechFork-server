package com.techfork.domain.activity.service;

import com.techfork.domain.activity.converter.ActivityConverter;
import com.techfork.domain.activity.dto.BookmarkDto;
import com.techfork.domain.activity.dto.BookmarkListResponse;
import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScrabPostRepository scrabPostRepository;

    @Mock
    private ActivityConverter activityConverter;

    @InjectMocks
    private ActivityQueryService activityQueryService;

    @Test
    @DisplayName("북마크 목록 조회 성공 - 첫 페이지")
    void getBookmarks_Success_FirstPage() {
        // Given
        Long userId = 1L;
        Long lastBookmarkId = null;
        int size = 20;

        User mockUser = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        List<BookmarkDto> mockBookmarks = Arrays.asList(
                new BookmarkDto(3L, 103L, "게시글3", "https://test.com/3", "회사A", "logo.png", LocalDateTime.now()),
                new BookmarkDto(2L, 102L, "게시글2", "https://test.com/2", "회사B", "logo.png", LocalDateTime.now()),
                new BookmarkDto(1L, 101L, "게시글1", "https://test.com/1", "회사C", "logo.png", LocalDateTime.now())
        );
        given(scrabPostRepository.findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class)))
                .willReturn(mockBookmarks);

        BookmarkListResponse expectedResponse = new BookmarkListResponse(mockBookmarks, 2L, true);
        given(activityConverter.toBookmarkListResponse(mockBookmarks, size)).willReturn(expectedResponse);

        // When
        BookmarkListResponse response = activityQueryService.getBookmarks(userId, lastBookmarkId, size);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.bookmarks()).hasSize(3);
        assertThat(response.lastBookmarkId()).isEqualTo(2L);
        assertThat(response.hasNext()).isTrue();

        verify(userRepository, times(1)).findById(userId);
        verify(scrabPostRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
        verify(activityConverter, times(1)).toBookmarkListResponse(mockBookmarks, size);
    }

    @Test
    @DisplayName("북마크 목록 조회 성공 - 커서 기반 페이징")
    void getBookmarks_Success_WithCursor() {
        // Given
        Long userId = 1L;
        Long lastBookmarkId = 10L;
        int size = 20;

        User mockUser = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        List<BookmarkDto> mockBookmarks = Arrays.asList(
                new BookmarkDto(9L, 109L, "게시글9", "https://test.com/9", "회사A", "logo.png", LocalDateTime.now()),
                new BookmarkDto(8L, 108L, "게시글8", "https://test.com/8", "회사B", "logo.png", LocalDateTime.now())
        );
        given(scrabPostRepository.findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class)))
                .willReturn(mockBookmarks);

        BookmarkListResponse expectedResponse = new BookmarkListResponse(mockBookmarks, null, false);
        given(activityConverter.toBookmarkListResponse(mockBookmarks, size)).willReturn(expectedResponse);

        // When
        BookmarkListResponse response = activityQueryService.getBookmarks(userId, lastBookmarkId, size);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.bookmarks()).hasSize(2);
        assertThat(response.lastBookmarkId()).isNull();
        assertThat(response.hasNext()).isFalse();

        verify(scrabPostRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
    }

    @Test
    @DisplayName("북마크 목록 조회 실패 - 존재하지 않는 사용자")
    void getBookmarks_Fail_UserNotFound() {
        // Given
        Long userId = 999L;
        Long lastBookmarkId = null;
        int size = 20;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityQueryService.getBookmarks(userId, lastBookmarkId, size))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(userRepository, times(1)).findById(userId);
        verify(scrabPostRepository, never()).findBookmarksWithCursor(any(), any(), any());
    }

    @Test
    @DisplayName("북마크 목록 조회 성공 - 빈 목록")
    void getBookmarks_Success_EmptyList() {
        // Given
        Long userId = 1L;
        Long lastBookmarkId = null;
        int size = 20;

        User mockUser = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        List<BookmarkDto> emptyBookmarks = List.of();
        given(scrabPostRepository.findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class)))
                .willReturn(emptyBookmarks);

        BookmarkListResponse expectedResponse = new BookmarkListResponse(emptyBookmarks, null, false);
        given(activityConverter.toBookmarkListResponse(emptyBookmarks, size)).willReturn(expectedResponse);

        // When
        BookmarkListResponse response = activityQueryService.getBookmarks(userId, lastBookmarkId, size);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.bookmarks()).isEmpty();
        assertThat(response.lastBookmarkId()).isNull();
        assertThat(response.hasNext()).isFalse();

        verify(scrabPostRepository, times(1)).findBookmarksWithCursor(eq(mockUser), eq(lastBookmarkId), any(PageRequest.class));
    }
}
