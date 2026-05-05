package com.techfork.activity.readpost.service;

import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.activity.readpost.converter.ReadPostConverter;
import com.techfork.activity.readpost.dto.ReadPostDto;
import com.techfork.activity.readpost.dto.ReadPostListResponse;
import com.techfork.activity.readpost.repository.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
class ReadPostQueryServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private ReadPostRepository readPostRepository;

    @Mock
    private PostKeywordRepository postKeywordRepository;

    @Mock
    private ReadPostConverter readPostConverter;

    @Mock
    private CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    @InjectMocks
    private ReadPostQueryService readPostQueryService;

    @BeforeEach
    void setUp() {
        lenient().when(thumbnailOptimizer.optimize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("읽은 게시글 목록 조회")
    class GetReadPosts {

        @Test
        @DisplayName("첫 페이지 요청 시 size+1 결과를 converter에 전달한다")
        void getReadPosts_Success_FirstPage() {
            Long userId = 1L;
            Long lastReadPostId = null;
            int size = 2;
            List<ReadPostDto> repositoryResult = Arrays.asList(
                    readPostDto(3L, 103L, "읽은 게시글 3", 100L, "thumb3.png", LocalDateTime.now().minusHours(1)),
                    readPostDto(2L, 102L, "읽은 게시글 2", 200L, "thumb2.png", LocalDateTime.now().minusHours(2)),
                    readPostDto(1L, 101L, "읽은 게시글 1", 300L, "thumb1.png", LocalDateTime.now().minusHours(3))
            );
            ReadPostListResponse expectedResponse = new ReadPostListResponse(repositoryResult.subList(0, 2), 2L, true);

            given(readPostRepository.findReadPostsWithCursor(eq(userId), eq(lastReadPostId), any(PageRequest.class)))
                    .willReturn(repositoryResult);
            given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of());
            given(bookmarkRepository.findBookmarkedPostIds(eq(userId), any())).willReturn(List.of());
            given(readPostConverter.toReadPostListResponse(any(), eq(size))).willReturn(expectedResponse);

            ReadPostListResponse response = readPostQueryService.getReadPosts(userId, lastReadPostId, size);

            assertThat(response).isEqualTo(expectedResponse);
            verify(readPostRepository, times(1)).findReadPostsWithCursor(eq(userId), eq(lastReadPostId), any(PageRequest.class));
            verify(readPostConverter, times(1)).toReadPostListResponse(any(), eq(size));
        }

        @Test
        @DisplayName("키워드와 북마크 여부를 함께 조합한다")
        void getReadPosts_Success_WithKeywordsAndBookmarks() {
            Long userId = 1L;
            Long lastReadPostId = null;
            int size = 20;
            LocalDateTime now = LocalDateTime.now();
            List<ReadPostDto> repositoryResult = Arrays.asList(
                    readPostDto(3L, 103L, "읽은 게시글 3", 100L, "thumb3.png", now.minusHours(1)),
                    readPostDto(2L, 102L, "읽은 게시글 2", 200L, "thumb2.png", now.minusHours(2))
            );

            given(readPostRepository.findReadPostsWithCursor(eq(userId), eq(lastReadPostId), any(PageRequest.class)))
                    .willReturn(repositoryResult);

            Post mockPost = mock(Post.class);
            given(mockPost.getId()).willReturn(103L);
            given(postKeywordRepository.findByPostIdIn(any())).willReturn(List.of(
                    PostKeyword.builder().keyword("Java").post(mockPost).build(),
                    PostKeyword.builder().keyword("Spring").post(mockPost).build()
            ));
            given(bookmarkRepository.findBookmarkedPostIds(eq(userId), any())).willReturn(List.of(103L));

            List<ReadPostDto> expectedReadPosts = Arrays.asList(
                    ReadPostDto.builder()
                            .readPostId(3L)
                            .postId(103L)
                            .title("읽은 게시글 3")
                            .shortSummary("요약 103")
                            .url("https://test.com/103")
                            .companyName("회사103")
                            .logoUrl("logo.png")
                            .publishedAt(repositoryResult.get(0).publishedAt())
                            .thumbnailUrl("thumb3.png")
                            .viewCount(100L)
                            .keywords(List.of("Java", "Spring"))
                            .isBookmarked(true)
                            .readAt(repositoryResult.get(0).readAt())
                            .build(),
                    ReadPostDto.builder()
                            .readPostId(2L)
                            .postId(102L)
                            .title("읽은 게시글 2")
                            .shortSummary("요약 102")
                            .url("https://test.com/102")
                            .companyName("회사102")
                            .logoUrl("logo.png")
                            .publishedAt(repositoryResult.get(1).publishedAt())
                            .thumbnailUrl("thumb2.png")
                            .viewCount(200L)
                            .keywords(List.of())
                            .isBookmarked(false)
                            .readAt(repositoryResult.get(1).readAt())
                            .build()
            );
            ReadPostListResponse expectedResponse = new ReadPostListResponse(expectedReadPosts, 2L, false);
            given(readPostConverter.toReadPostListResponse(any(), eq(size))).willReturn(expectedResponse);

            ReadPostListResponse response = readPostQueryService.getReadPosts(userId, lastReadPostId, size);

            assertThat(response.readPosts()).hasSize(2);
            assertThat(response.readPosts().get(0).keywords()).containsExactlyInAnyOrder("Java", "Spring");
            assertThat(response.readPosts().get(0).isBookmarked()).isTrue();
            assertThat(response.readPosts().get(1).isBookmarked()).isFalse();
        }

        @Test
        @DisplayName("빈 목록이면 추가 조회 없이 바로 응답을 조립한다")
        void getReadPosts_Success_EmptyList() {
            Long userId = 1L;
            Long lastReadPostId = null;
            int size = 20;
            List<ReadPostDto> emptyReadPosts = List.of();
            ReadPostListResponse expectedResponse = new ReadPostListResponse(emptyReadPosts, null, false);

            given(readPostRepository.findReadPostsWithCursor(eq(userId), eq(lastReadPostId), any(PageRequest.class)))
                    .willReturn(emptyReadPosts);
            given(readPostConverter.toReadPostListResponse(emptyReadPosts, size)).willReturn(expectedResponse);

            ReadPostListResponse response = readPostQueryService.getReadPosts(userId, lastReadPostId, size);

            assertThat(response).isEqualTo(expectedResponse);
            verify(postKeywordRepository, never()).findByPostIdIn(any());
            verify(bookmarkRepository, never()).findBookmarkedPostIds(any(), any());
        }
    }

    private ReadPostDto readPostDto(Long readPostId, Long postId, String title, Long viewCount, String thumbnailUrl, LocalDateTime readAt) {
        return ReadPostDto.builder()
                .readPostId(readPostId)
                .postId(postId)
                .title(title)
                .shortSummary("요약 " + postId)
                .url("https://test.com/" + postId)
                .companyName("회사" + postId)
                .logoUrl("logo.png")
                .publishedAt(readAt.minusDays(1))
                .thumbnailUrl(thumbnailUrl)
                .viewCount(viewCount)
                .keywords(null)
                .isBookmarked(null)
                .readAt(readAt)
                .build();
    }
}
