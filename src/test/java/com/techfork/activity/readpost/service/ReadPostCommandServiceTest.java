package com.techfork.activity.readpost.service;

import com.techfork.activity.readpost.domain.ReadPostFirstReadPolicy;
import com.techfork.activity.readpost.dto.ReadPostRequest;
import com.techfork.activity.readpost.entity.ReadPost;
import com.techfork.activity.readpost.repository.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.exception.PostErrorCode;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadPostCommandServiceTest {

    @Mock
    private ReadPostRepository readPostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReadPostFirstReadPolicy readPostFirstReadPolicy;

    @InjectMocks
    private ReadPostCommandService readPostCommandService;

    @Nested
    @DisplayName("읽은 게시글 저장")
    class SaveReadPost {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("처음 읽는 게시글이면 조회수가 증가하고 요청 상태가 그대로 저장된다")
            void saveReadPost_FirstRead_IncrementViewCountAndPreserveState() {
                Long userId = 1L;
                Long postId = 100L;
                LocalDateTime readAt = LocalDateTime.of(2026, 5, 5, 14, 0, 0);
                Integer readDurationSeconds = 300;

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
                ReadPostRequest request = new ReadPostRequest(postId, readAt, readDurationSeconds);

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
                given(readPostFirstReadPolicy.isFirstRead(mockUser, mockPost)).willReturn(true);
                given(readPostRepository.save(any(ReadPost.class))).willReturn(mock(ReadPost.class));

                Long beforeViewCount = mockPost.getViewCount();

                readPostCommandService.saveReadPost(userId, request);

                ArgumentCaptor<ReadPost> readPostCaptor = ArgumentCaptor.forClass(ReadPost.class);
                verify(readPostFirstReadPolicy, times(1)).isFirstRead(mockUser, mockPost);
                verify(readPostRepository, times(1)).save(readPostCaptor.capture());
                ReadPost savedReadPost = readPostCaptor.getValue();

                assertThat(mockPost.getViewCount()).isEqualTo(beforeViewCount + 1);
                assertThat(savedReadPost.getUser()).isSameAs(mockUser);
                assertThat(savedReadPost.getPost()).isSameAs(mockPost);
                assertThat(savedReadPost.getReadAt()).isEqualTo(readAt);
                assertThat(savedReadPost.getReadDurationSeconds()).isEqualTo(readDurationSeconds);
            }

            @Test
            @DisplayName("이미 읽은 게시글이면 조회수는 증가하지 않지만 기록은 저장된다")
            void saveReadPost_AlreadyRead_NoIncrementViewCount() {
                Long userId = 1L;
                Long postId = 100L;
                LocalDateTime readAt = LocalDateTime.of(2026, 5, 5, 15, 0, 0);
                Integer readDurationSeconds = 400;

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
                ReadPostRequest request = new ReadPostRequest(postId, readAt, readDurationSeconds);

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
                given(readPostFirstReadPolicy.isFirstRead(mockUser, mockPost)).willReturn(false);
                given(readPostRepository.save(any(ReadPost.class))).willReturn(mock(ReadPost.class));

                Long beforeViewCount = mockPost.getViewCount();

                readPostCommandService.saveReadPost(userId, request);

                ArgumentCaptor<ReadPost> readPostCaptor = ArgumentCaptor.forClass(ReadPost.class);
                verify(readPostFirstReadPolicy, times(1)).isFirstRead(mockUser, mockPost);
                verify(readPostRepository, times(1)).save(readPostCaptor.capture());

                assertThat(mockPost.getViewCount()).isEqualTo(beforeViewCount);
                assertThat(readPostCaptor.getValue().getReadDurationSeconds()).isEqualTo(readDurationSeconds);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 사용자면 저장에 실패한다")
            void saveReadPost_Fail_UserNotFound() {
                Long userId = 999L;
                ReadPostRequest request = new ReadPostRequest(100L, LocalDateTime.now(), 300);

                given(userRepository.findById(userId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> readPostCommandService.saveReadPost(userId, request))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

                verify(postRepository, never()).findById(any());
                verify(readPostFirstReadPolicy, never()).isFirstRead(any(), any());
                verify(readPostRepository, never()).save(any());
            }

            @Test
            @DisplayName("존재하지 않는 게시글이면 저장에 실패한다")
            void saveReadPost_Fail_PostNotFound() {
                Long userId = 1L;
                Long postId = 999L;
                ReadPostRequest request = new ReadPostRequest(postId, LocalDateTime.now(), 300);
                User mockUser = mock(User.class);

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(postRepository.findById(postId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> readPostCommandService.saveReadPost(userId, request))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", PostErrorCode.POST_NOT_FOUND);

                verify(readPostFirstReadPolicy, never()).isFirstRead(any(), any());
                verify(readPostRepository, never()).save(any());
            }
        }
    }
}
