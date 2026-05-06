package com.techfork.activity.readpost.application.query;

import com.techfork.activity.bookmark.application.query.lookup.BookmarkLookupService;
import com.techfork.activity.readpost.infrastructure.ReadPostQueryRow;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.domain.post.service.PostKeywordLookupService;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReadPostQueryServiceTest {

    @Mock
    private BookmarkLookupService bookmarkLookupService;

    @Mock
    private ReadPostRepository readPostRepository;

    @Mock
    private PostKeywordLookupService postKeywordLookupService;

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
            GetReadPostsQuery query = new GetReadPostsQuery(userId, lastReadPostId, size);
            List<ReadPostQueryRow> repositoryResult = Arrays.asList(
                    readPostRow(3L, 103L, "읽은 게시글 3", 100L, "thumb3.png", LocalDateTime.now().minusHours(1)),
                    readPostRow(2L, 102L, "읽은 게시글 2", 200L, "thumb2.png", LocalDateTime.now().minusHours(2)),
                    readPostRow(1L, 101L, "읽은 게시글 1", 300L, "thumb1.png", LocalDateTime.now().minusHours(3))
            );

            given(readPostRepository.findReadPostsWithCursor(eq(userId), eq(lastReadPostId), any(PageRequest.class)))
                    .willReturn(repositoryResult);
            given(postKeywordLookupService.getKeywordsByPostIds(any())).willReturn(Map.of());
            given(bookmarkLookupService.getBookmarkedPostIds(eq(userId), any())).willReturn(java.util.Set.of());

            GetReadPostsResult response = readPostQueryService.getReadPosts(query);

            assertThat(response.readPosts()).hasSize(2);
            assertThat(response.lastReadPostId()).isEqualTo(2L);
            assertThat(response.hasNext()).isTrue();
            verify(readPostRepository, times(1)).findReadPostsWithCursor(eq(userId), eq(lastReadPostId), any(PageRequest.class));
        }

        @Test
        @DisplayName("키워드와 북마크 여부를 함께 조합한다")
        void getReadPosts_Success_WithKeywordsAndBookmarks() {
            Long userId = 1L;
            Long lastReadPostId = null;
            int size = 20;
            GetReadPostsQuery query = new GetReadPostsQuery(userId, lastReadPostId, size);
            LocalDateTime now = LocalDateTime.now();
            List<ReadPostQueryRow> repositoryResult = Arrays.asList(
                    readPostRow(3L, 103L, "읽은 게시글 3", 100L, "thumb3.png", now.minusHours(1)),
                    readPostRow(2L, 102L, "읽은 게시글 2", 200L, "thumb2.png", now.minusHours(2))
            );

            given(readPostRepository.findReadPostsWithCursor(eq(userId), eq(lastReadPostId), any(PageRequest.class)))
                    .willReturn(repositoryResult);

            given(postKeywordLookupService.getKeywordsByPostIds(any()))
                    .willReturn(Map.of(103L, List.of("Java", "Spring")));
            given(bookmarkLookupService.getBookmarkedPostIds(eq(userId), any())).willReturn(java.util.Set.of(103L));

            GetReadPostsResult response = readPostQueryService.getReadPosts(query);

            assertThat(response.readPosts()).hasSize(2);
            assertThat(response.readPosts().get(0).keywords()).containsExactlyInAnyOrder("Java", "Spring");
            assertThat(response.readPosts().get(0).isBookmarked()).isTrue();
            assertThat(response.readPosts().get(1).isBookmarked()).isFalse();
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("빈 목록이면 추가 조회 없이 바로 응답을 조립한다")
        void getReadPosts_Success_EmptyList() {
            Long userId = 1L;
            Long lastReadPostId = null;
            int size = 20;
            GetReadPostsQuery query = new GetReadPostsQuery(userId, lastReadPostId, size);
            List<ReadPostQueryRow> emptyReadPosts = List.of();

            given(readPostRepository.findReadPostsWithCursor(eq(userId), eq(lastReadPostId), any(PageRequest.class)))
                    .willReturn(emptyReadPosts);

            GetReadPostsResult response = readPostQueryService.getReadPosts(query);

            assertThat(response.readPosts()).isEmpty();
            assertThat(response.lastReadPostId()).isNull();
            assertThat(response.hasNext()).isFalse();
            verify(postKeywordLookupService, never()).getKeywordsByPostIds(any());
            verify(bookmarkLookupService, never()).getBookmarkedPostIds(any(), any());
        }
    }

    private ReadPostQueryRow readPostRow(Long readPostId, Long postId, String title, Long viewCount, String thumbnailUrl, LocalDateTime readAt) {
        return new ReadPostQueryRow(
                readPostId,
                postId,
                title,
                "요약 " + postId,
                "https://test.com/" + postId,
                "회사" + postId,
                "logo.png",
                readAt.minusDays(1),
                thumbnailUrl,
                viewCount,
                readAt
        );
    }
}
