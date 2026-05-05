package com.techfork.domain.activity.service;

import com.techfork.domain.activity.dto.BookmarkRequest;
import com.techfork.domain.activity.dto.ReadPostRequest;
import com.techfork.domain.activity.dto.SearchHistoryRequest;
import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.entity.Bookmark;
import com.techfork.domain.activity.entity.SearchHistory;
import com.techfork.domain.activity.exception.ActivityErrorCode;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.repository.BookmarkRepository;
import com.techfork.domain.activity.repository.SearchHistoryRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.exception.PostErrorCode;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityCommandServiceTest {

    @Mock
    private ReadPostRepository readPostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private ActivityCommandService activityCommandService;

    @Test
    @DisplayName("처음 읽는 게시글이면 조회수가 증가한다")
    void saveReadPost_FirstRead_IncrementViewCount() {
        // Given: 테스트 데이터 준비
        Long userId = 1L;
        Long postId = 100L;

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

        ReadPostRequest request = new ReadPostRequest(
                postId,
                LocalDateTime.now(),
                300
        );

        // Mock 동작 정의
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(readPostRepository.existsByUserAndPost(mockUser, mockPost)).willReturn(false); // 처음 읽음
        given(readPostRepository.save(any(ReadPost.class))).willReturn(mock(ReadPost.class));

        Long beforeViewCount = mockPost.getViewCount();

        // When: saveReadPost 호출
        activityCommandService.saveReadPost(userId, request);

        // Then: 조회수가 1 증가했는지 검증
        assertThat(mockPost.getViewCount()).isEqualTo(beforeViewCount + 1);

        // 그리고 ReadPost가 저장되었는지 검증
        verify(readPostRepository, times(1)).save(any(ReadPost.class));
    }

    @Test
    @DisplayName("이미 읽은 게시글이면 조회수가 증가하지 않는다")
    void saveReadPost_AlreadyRead_NoIncrementViewCount() {
        // Given
        Long userId = 1L;
        Long postId = 100L;

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

        ReadPostRequest request = new ReadPostRequest(
                postId,
                LocalDateTime.now(),
                300
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(readPostRepository.existsByUserAndPost(mockUser, mockPost)).willReturn(true); // 이미 읽음
        given(readPostRepository.save(any(ReadPost.class))).willReturn(mock(ReadPost.class));

        Long beforeViewCount = mockPost.getViewCount();

        // When
        activityCommandService.saveReadPost(userId, request);

        // Then: 조회수가 증가하지 않음
        assertThat(mockPost.getViewCount()).isEqualTo(beforeViewCount);

        // 하지만 ReadPost는 저장됨 (읽은 기록은 매번 저장)
        verify(readPostRepository, times(1)).save(any(ReadPost.class));
    }

    // ===== 북마크 추가 테스트 =====

    @Test
    @DisplayName("북마크 추가 성공")
    void addBookmark_Success() {
        // Given
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

        // When
        activityCommandService.addBookmark(userId, request);

        LocalDateTime afterInvocation = LocalDateTime.now();

        // Then
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
        // Given
        Long userId = 1L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

        User mockUser = mock(User.class);
        Post mockPost = mock(Post.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(bookmarkRepository.existsByUserAndPost(mockUser, mockPost)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> activityCommandService.addBookmark(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", ActivityErrorCode.BOOKMARK_ALREADY_EXISTS);

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("북마크 추가 실패 - 존재하지 않는 게시글")
    void addBookmark_Fail_PostNotFound() {
        // Given
        Long userId = 1L;
        Long postId = 999L;
        BookmarkRequest request = new BookmarkRequest(postId);

        User mockUser = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityCommandService.addBookmark(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", PostErrorCode.POST_NOT_FOUND);

        verify(bookmarkRepository, never()).save(any());
    }

    // ===== 북마크 삭제 테스트 =====

    @Test
    @DisplayName("북마크 삭제 성공")
    void deleteBookmark_Success() {
        // Given
        Long userId = 1L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

        User mockUser = mock(User.class);
        Post mockPost = mock(Post.class);
        Bookmark mockBookmark = mock(Bookmark.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(bookmarkRepository.findByUserAndPost(mockUser, mockPost)).willReturn(Optional.of(mockBookmark));

        // When
        activityCommandService.deleteBookmark(userId, request);

        // Then
        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, times(1)).findById(postId);
        verify(bookmarkRepository, times(1)).findByUserAndPost(mockUser, mockPost);
        verify(bookmarkRepository, times(1)).delete(mockBookmark);
    }

    @Test
    @DisplayName("북마크 삭제 실패 - 북마크가 존재하지 않음")
    void deleteBookmark_Fail_NotFound() {
        // Given
        Long userId = 1L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

        User mockUser = mock(User.class);
        Post mockPost = mock(Post.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(bookmarkRepository.findByUserAndPost(mockUser, mockPost)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityCommandService.deleteBookmark(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", ActivityErrorCode.BOOKMARK_NOT_FOUND);

        verify(bookmarkRepository, never()).delete(any());
    }

    @Test
    @DisplayName("북마크 삭제 실패 - 존재하지 않는 사용자")
    void deleteBookmark_Fail_UserNotFound() {
        // Given
        Long userId = 999L;
        Long postId = 100L;
        BookmarkRequest request = new BookmarkRequest(postId);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityCommandService.deleteBookmark(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(bookmarkRepository, never()).delete(any());
    }

    @Test
    @DisplayName("읽은 게시글 저장 실패 - 존재하지 않는 사용자")
    void saveReadPost_Fail_UserNotFound() {
        // Given
        Long userId = 999L;
        Long postId = 100L;
        ReadPostRequest request = new ReadPostRequest(postId, LocalDateTime.now(), 300);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityCommandService.saveReadPost(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, never()).findById(any());
        verify(readPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("읽은 게시글 저장 실패 - 존재하지 않는 게시글")
    void saveReadPost_Fail_PostNotFound() {
        // Given
        Long userId = 1L;
        Long postId = 999L;
        User mockUser = mock(User.class);
        ReadPostRequest request = new ReadPostRequest(postId, LocalDateTime.now(), 300);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityCommandService.saveReadPost(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", PostErrorCode.POST_NOT_FOUND);

        verify(postRepository, times(1)).findById(postId);
        verify(readPostRepository, never()).save(any());
    }

    // ===== 검색 히스토리 테스트 =====

    @Test
    @DisplayName("검색 히스토리 저장 성공")
    void saveSearchHistory_Success() {
        // Given
        Long userId = 1L;
        String query = "Spring Boot";
        LocalDateTime searchedAt = LocalDateTime.now();
        SearchHistoryRequest request = new SearchHistoryRequest(query, searchedAt);

        User mockUser = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(searchHistoryRepository.save(any(SearchHistory.class))).willReturn(mock(SearchHistory.class));

        // When
        activityCommandService.saveSearchHistory(userId, request);

        // Then
        verify(userRepository, times(1)).findById(userId);
        ArgumentCaptor<SearchHistory> searchHistoryCaptor = ArgumentCaptor.forClass(SearchHistory.class);
        verify(searchHistoryRepository, times(1)).save(searchHistoryCaptor.capture());
        assertThat(searchHistoryCaptor.getValue().getQuery()).isEqualTo(query);
    }

    @Test
    @DisplayName("검색 히스토리 저장 실패 - 존재하지 않는 사용자")
    void saveSearchHistory_Fail_UserNotFound() {
        // Given
        Long userId = 999L;
        SearchHistoryRequest request = new SearchHistoryRequest("Spring Boot", LocalDateTime.now());

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityCommandService.saveSearchHistory(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(searchHistoryRepository, never()).save(any());
    }

}
