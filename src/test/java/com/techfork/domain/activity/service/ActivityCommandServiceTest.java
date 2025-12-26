package com.techfork.domain.activity.service;

import com.techfork.domain.activity.dto.ReadPostRequest;
import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.activity.repository.SearchHistoryRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ActivityCommandService 단위 테스트
 * - Mockito를 사용해서 의존성을 Mock으로 대체
 * - 실제 DB 없이 빠르게 테스트
 */
@ExtendWith(MockitoExtension.class)
class ActivityCommandServiceTest {

    @Mock
    private ReadPostRepository readPostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

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
}
