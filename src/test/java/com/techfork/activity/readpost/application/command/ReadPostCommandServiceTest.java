package com.techfork.activity.readpost.application.command;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.readpost.domain.ReadPostErrorCode;
import com.techfork.activity.readpost.domain.ReadPostFirstReadPolicy;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.post.application.command.PostViewCountCommandService;
import com.techfork.post.domain.Post;
import com.techfork.post.domain.exception.PostErrorCode;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.application.query.lookup.PostLookupService;
import com.techfork.domain.source.fixture.TechBlogFixture;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.global.exception.GeneralException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private PostLookupService postLookupService;

    @Mock
    private PostViewCountCommandService postViewCountCommandService;

    @Mock
    private UserLookupService userLookupService;

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
                Post mockPost = PostFixture.createPost(
                        TechBlogFixture.createTechBlog("테스트회사", "https://test.com"),
                        "테스트 제목",
                        "https://test.com/post/1"
                );
                ReflectionTestUtils.setField(mockPost, "id", postId);
                SaveReadPostCommand command = new SaveReadPostCommand(userId, postId, readAt, readDurationSeconds);

                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId)).willReturn(mockPost);
                given(readPostFirstReadPolicy.markFirstRead(mockUser, mockPost, readAt)).willReturn(true);
                given(postViewCountCommandService.incrementViewCount(postId)).willReturn(true);
                given(readPostRepository.save(any(ReadPost.class))).willReturn(mock(ReadPost.class));

                Long beforeViewCount = mockPost.getViewCount();

                readPostCommandService.saveReadPost(command);

                ArgumentCaptor<ReadPost> readPostCaptor = ArgumentCaptor.forClass(ReadPost.class);
                verify(readPostFirstReadPolicy, times(1)).markFirstRead(mockUser, mockPost, readAt);
                verify(postViewCountCommandService, times(1)).incrementViewCount(postId);
                verify(readPostRepository, times(1)).save(readPostCaptor.capture());
                ReadPost savedReadPost = readPostCaptor.getValue();

                assertThat(mockPost.getViewCount()).isEqualTo(beforeViewCount);
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
                Post mockPost = PostFixture.createPost(
                        TechBlogFixture.createTechBlog("테스트회사", "https://test.com"),
                        "테스트 제목",
                        "https://test.com/post/1"
                );
                SaveReadPostCommand command = new SaveReadPostCommand(userId, postId, readAt, readDurationSeconds);

                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId)).willReturn(mockPost);
                given(readPostFirstReadPolicy.markFirstRead(mockUser, mockPost, readAt)).willReturn(false);
                given(readPostRepository.save(any(ReadPost.class))).willReturn(mock(ReadPost.class));

                Long beforeViewCount = mockPost.getViewCount();

                readPostCommandService.saveReadPost(command);

                ArgumentCaptor<ReadPost> readPostCaptor = ArgumentCaptor.forClass(ReadPost.class);
                verify(readPostFirstReadPolicy, times(1)).markFirstRead(mockUser, mockPost, readAt);
                verify(postViewCountCommandService, never()).incrementViewCount(any());
                verify(readPostRepository, times(1)).save(readPostCaptor.capture());

                assertThat(mockPost.getViewCount()).isEqualTo(beforeViewCount);
                assertThat(readPostCaptor.getValue().getReadDurationSeconds()).isEqualTo(readDurationSeconds);
            }

            @Test
            @DisplayName("첫 읽기인데 조회수 증가에 실패하면 예외가 발생하고 기록을 저장하지 않는다")
            void saveReadPost_FirstRead_ViewCountIncrementFailed_ThrowException() {
                Long userId = 1L;
                Long postId = 100L;
                LocalDateTime readAt = LocalDateTime.of(2026, 5, 5, 16, 0, 0);

                User mockUser = mock(User.class);
                Post mockPost = PostFixture.createPost(
                        TechBlogFixture.createTechBlog("테스트회사", "https://test.com"),
                        "테스트 제목",
                        "https://test.com/post/1"
                );
                ReflectionTestUtils.setField(mockPost, "id", postId);
                SaveReadPostCommand command = new SaveReadPostCommand(userId, postId, readAt, 300);

                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId)).willReturn(mockPost);
                given(readPostFirstReadPolicy.markFirstRead(mockUser, mockPost, readAt)).willReturn(true);
                given(postViewCountCommandService.incrementViewCount(postId)).willReturn(false);

                assertThatThrownBy(() -> readPostCommandService.saveReadPost(command))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", ReadPostErrorCode.READ_POST_VIEW_COUNT_INCREMENT_FAILED);

                verify(readPostFirstReadPolicy, times(1)).markFirstRead(mockUser, mockPost, readAt);
                verify(postViewCountCommandService, times(1)).incrementViewCount(postId);
                verify(readPostRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 사용자면 저장에 실패한다")
            void saveReadPost_Fail_UserNotFound() {
                Long userId = 999L;
                SaveReadPostCommand command = new SaveReadPostCommand(userId, 100L, LocalDateTime.now(), 300);

                given(userLookupService.getUserOrThrow(userId))
                        .willThrow(new GeneralException(UserErrorCode.USER_NOT_FOUND));

                assertThatThrownBy(() -> readPostCommandService.saveReadPost(command))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

                verify(postLookupService, never()).getPostOrThrow(any());
                verify(postViewCountCommandService, never()).incrementViewCount(any());
                verify(readPostFirstReadPolicy, never()).markFirstRead(any(), any(), any());
                verify(readPostRepository, never()).save(any());
            }

            @Test
            @DisplayName("존재하지 않는 게시글이면 저장에 실패한다")
            void saveReadPost_Fail_PostNotFound() {
                Long userId = 1L;
                Long postId = 999L;
                SaveReadPostCommand command = new SaveReadPostCommand(userId, postId, LocalDateTime.now(), 300);
                User mockUser = mock(User.class);

                given(userLookupService.getUserOrThrow(userId)).willReturn(mockUser);
                given(postLookupService.getPostOrThrow(postId))
                        .willThrow(new GeneralException(PostErrorCode.POST_NOT_FOUND));

                assertThatThrownBy(() -> readPostCommandService.saveReadPost(command))
                        .isInstanceOf(GeneralException.class)
                        .hasFieldOrPropertyWithValue("code", PostErrorCode.POST_NOT_FOUND);

                verify(postViewCountCommandService, never()).incrementViewCount(any());
                verify(readPostFirstReadPolicy, never()).markFirstRead(any(), any(), any());
                verify(readPostRepository, never()).save(any());
            }
        }
    }
}
