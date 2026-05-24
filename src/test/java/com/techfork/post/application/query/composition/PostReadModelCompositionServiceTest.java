package com.techfork.post.application.query.composition;

import com.techfork.activity.bookmark.application.query.lookup.BookmarkLookupService;
import com.techfork.global.util.CloudflareThirdPartyThumbnailOptimizer;
import com.techfork.post.application.query.lookup.PostKeywordLookupService;
import com.techfork.post.application.query.result.GetPostDetailResult;
import com.techfork.post.infrastructure.row.PostDetailRow;
import com.techfork.post.infrastructure.row.PostInfoRow;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostReadModelCompositionServiceTest {

    @Mock
    private PostKeywordLookupService postKeywordLookupService;

    @Mock
    private BookmarkLookupService bookmarkLookupService;

    @Mock
    private CloudflareThirdPartyThumbnailOptimizer thumbnailOptimizer;

    @InjectMocks
    private PostReadModelCompositionService postReadModelCompositionService;

    @Nested
    @DisplayName("게시글 목록 조합")
    class ComposePostInfoRows {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("빈 목록이면 추가 조회 없이 빈 목록을 반환한다")
            void composePostInfoRows_EmptyList_ReturnsEmpty() {
                List<PostInfoRow> result = postReadModelCompositionService.composePostInfoRows(List.of(), 1L);

                assertThat(result).isEmpty();
                verify(postKeywordLookupService, never()).getKeywordsByPostIds(List.of());
                verify(bookmarkLookupService, never()).getBookmarkedPostIds(1L, List.of());
            }

            @Test
            @DisplayName("비로그인 목록 조회는 keywords와 optimized thumbnail만 조합한다")
            void composePostInfoRows_WithoutUserId_ComposesKeywordsAndThumbnail() {
                List<PostInfoRow> posts = List.of(
                        postInfoRow(1L, "thumb-1"),
                        postInfoRow(2L, "thumb-2")
                );
                given(postKeywordLookupService.getKeywordsByPostIds(List.of(1L, 2L)))
                        .willReturn(Map.of(1L, List.of("Java"), 2L, List.of("Spring", "Boot")));
                given(thumbnailOptimizer.optimize("thumb-1")).willReturn("optimized-1");
                given(thumbnailOptimizer.optimize("thumb-2")).willReturn("optimized-2");

                List<PostInfoRow> result = postReadModelCompositionService.composePostInfoRows(posts, null);

                assertThat(result).hasSize(2);
                assertThat(result.get(0).keywords()).containsExactly("Java");
                assertThat(result.get(0).thumbnailUrl()).isEqualTo("optimized-1");
                assertThat(result.get(0).isBookmarked()).isNull();
                assertThat(result.get(1).keywords()).containsExactly("Spring", "Boot");
                assertThat(result.get(1).thumbnailUrl()).isEqualTo("optimized-2");
                assertThat(result.get(1).isBookmarked()).isNull();
                verify(postKeywordLookupService, times(1)).getKeywordsByPostIds(List.of(1L, 2L));
                verify(bookmarkLookupService, never()).getBookmarkedPostIds(1L, List.of(1L, 2L));
            }

            @Test
            @DisplayName("로그인 목록 조회는 keywords와 bookmark 여부를 함께 조합한다")
            void composePostInfoRows_WithUserId_ComposesKeywordsBookmarksAndThumbnail() {
                Long userId = 10L;
                List<PostInfoRow> posts = List.of(
                        postInfoRow(1L, "thumb-1"),
                        postInfoRow(2L, "thumb-2")
                );
                given(postKeywordLookupService.getKeywordsByPostIds(List.of(1L, 2L)))
                        .willReturn(Map.of(1L, List.of("Java")));
                given(bookmarkLookupService.getBookmarkedPostIds(userId, List.of(1L, 2L)))
                        .willReturn(Set.of(2L));
                given(thumbnailOptimizer.optimize("thumb-1")).willReturn("optimized-1");
                given(thumbnailOptimizer.optimize("thumb-2")).willReturn("optimized-2");

                List<PostInfoRow> result = postReadModelCompositionService.composePostInfoRows(posts, userId);

                assertThat(result).hasSize(2);
                assertThat(result.get(0).keywords()).containsExactly("Java");
                assertThat(result.get(0).isBookmarked()).isFalse();
                assertThat(result.get(1).keywords()).isEmpty();
                assertThat(result.get(1).isBookmarked()).isTrue();
                assertThat(result.get(1).thumbnailUrl()).isEqualTo("optimized-2");
                verify(postKeywordLookupService, times(1)).getKeywordsByPostIds(List.of(1L, 2L));
                verify(bookmarkLookupService, times(1)).getBookmarkedPostIds(userId, List.of(1L, 2L));
            }
        }
    }

    @Nested
    @DisplayName("게시글 상세 조합")
    class ComposePostDetail {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("비로그인 상세 조회는 keywords만 조합한다")
            void composePostDetail_WithoutUserId_ComposesKeywordsOnly() {
                Long postId = 1L;
                PostDetailRow postDetailRow = postDetailRow(postId);
                given(postKeywordLookupService.getKeywordsByPostIds(List.of(postId)))
                        .willReturn(Map.of(postId, List.of("Java", "Spring")));

                GetPostDetailResult result = postReadModelCompositionService.composePostDetail(postDetailRow, null);

                assertThat(result.id()).isEqualTo(postId);
                assertThat(result.keywords()).containsExactly("Java", "Spring");
                assertThat(result.isBookmarked()).isNull();
                verify(postKeywordLookupService, times(1)).getKeywordsByPostIds(List.of(postId));
                verify(bookmarkLookupService, never()).getBookmarkedPostIds(1L, List.of(postId));
            }

            @Test
            @DisplayName("로그인 상세 조회는 bookmark 여부를 함께 조합한다")
            void composePostDetail_WithUserId_ComposesBookmarkStatus() {
                Long postId = 1L;
                Long userId = 20L;
                PostDetailRow postDetailRow = postDetailRow(postId);
                given(postKeywordLookupService.getKeywordsByPostIds(List.of(postId)))
                        .willReturn(Map.of(postId, List.of("Kotlin")));
                given(bookmarkLookupService.getBookmarkedPostIds(userId, List.of(postId)))
                        .willReturn(Set.of(postId));

                GetPostDetailResult result = postReadModelCompositionService.composePostDetail(postDetailRow, userId);

                assertThat(result.keywords()).containsExactly("Kotlin");
                assertThat(result.isBookmarked()).isTrue();
                verify(postKeywordLookupService, times(1)).getKeywordsByPostIds(List.of(postId));
                verify(bookmarkLookupService, times(1)).getBookmarkedPostIds(userId, List.of(postId));
            }
        }
    }

    private PostInfoRow postInfoRow(Long postId, String thumbnailUrl) {
        return PostInfoRow.builder()
                .id(postId)
                .title("게시글 " + postId)
                .shortSummary("요약 " + postId)
                .company("카카오")
                .url("https://test.com/" + postId)
                .logoUrl("https://test.com/logo-" + postId + ".png")
                .thumbnailUrl(thumbnailUrl)
                .publishedAt(LocalDateTime.now())
                .viewCount(100L + postId)
                .keywords(null)
                .isBookmarked(null)
                .build();
    }

    private PostDetailRow postDetailRow(Long postId) {
        return PostDetailRow.builder()
                .id(postId)
                .title("게시글 " + postId)
                .summary("상세 요약 " + postId)
                .company("카카오")
                .url("https://test.com/" + postId)
                .logoUrl("https://test.com/logo-" + postId + ".png")
                .publishedAt(LocalDateTime.now())
                .viewCount(100L)
                .build();
    }
}
