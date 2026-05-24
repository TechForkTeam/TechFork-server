package com.techfork.post.application.query;

import com.techfork.global.exception.GeneralException;
import com.techfork.post.application.query.composition.PostReadModelCompositionService;
import com.techfork.post.application.query.result.CompanyListItemResult;
import com.techfork.post.application.query.result.GetCompanyListResult;
import com.techfork.post.application.query.result.GetPostDetailResult;
import com.techfork.post.application.query.result.GetPostListResult;
import com.techfork.post.domain.enums.EPostSortType;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.post.infrastructure.row.CompanyRow;
import com.techfork.post.infrastructure.row.PostDetailRow;
import com.techfork.post.infrastructure.row.PostInfoRow;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostReadModelCompositionService postReadModelCompositionService;

    @InjectMocks
    private PostQueryService postQueryService;

    @Nested
    @DisplayName("회사 목록 조회")
    class GetCompanies {

        @Test
        @DisplayName("회사 목록 조회 성공")
        void getCompanies_Success() {
            List<String> mockCompanies = List.of("카카오", "네이버", "라인");
            given(postRepository.findDistinctCompanies()).willReturn(mockCompanies);

            GetCompanyListResult result = postQueryService.getCompanies();

            assertThat(result.companies())
                    .extracting(CompanyListItemResult::company)
                    .containsExactly("카카오", "네이버", "라인");
            verify(postRepository, times(1)).findDistinctCompanies();
        }
    }

    @Nested
    @DisplayName("회사 상세 목록 조회 V2")
    class GetCompaniesV2 {

        @Test
        @DisplayName("회사 상세 정보 포함 목록 조회 성공")
        void getCompaniesV2_Success() {
            List<CompanyRow> companies = List.of(
                    CompanyRow.builder().company("카카오").hasNewPost(true).logoUrl("kakao-logo").build(),
                    CompanyRow.builder().company("네이버").hasNewPost(false).logoUrl("naver-logo").build()
            );
            given(postRepository.findCompaniesWithDetails()).willReturn(companies);

            GetCompanyListResult result = postQueryService.getCompaniesV2();

            assertThat(result.totalNumber()).isEqualTo(2);
            assertThat(result.companies()).extracting(CompanyListItemResult::company)
                    .containsExactly("카카오", "네이버");
            assertThat(result.companies().get(0).hasNewPost()).isTrue();
            assertThat(result.companies().get(1).hasNewPost()).isFalse();
            verify(postRepository, times(1)).findCompaniesWithDetails();
        }
    }

    @Nested
    @DisplayName("게시글 상세 조회")
    class GetPostDetail {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("조회 row를 조합 서비스에 위임한다")
            void getPostDetail_DelegatesToCompositionService() {
                Long postId = 1L;
                Long userId = 100L;
                PostDetailRow postDetailRow = PostDetailRow.builder()
                        .id(postId)
                        .title("테스트 제목")
                        .summary("테스트 요약")
                        .company("카카오")
                        .url("https://test.com/1")
                        .logoUrl("https://test.com/logo.png")
                        .publishedAt(LocalDateTime.now())
                        .viewCount(100L)
                        .build();
                GetPostDetailResult expected = GetPostDetailResult.builder()
                        .id(postId)
                        .title("테스트 제목")
                        .summary("테스트 요약")
                        .company("카카오")
                        .url("https://test.com/1")
                        .logoUrl("https://test.com/logo.png")
                        .publishedAt(postDetailRow.publishedAt())
                        .viewCount(100L)
                        .keywords(List.of("Java"))
                        .isBookmarked(true)
                        .build();

                given(postRepository.findByIdWithTechBlog(postId)).willReturn(Optional.of(postDetailRow));
                given(postReadModelCompositionService.composePostDetail(postDetailRow, userId)).willReturn(expected);

                GetPostDetailResult result = postQueryService.getPostDetail(new GetPostDetailQuery(postId, userId));

                assertThat(result).isEqualTo(expected);
                verify(postRepository, times(1)).findByIdWithTechBlog(postId);
                verify(postReadModelCompositionService, times(1)).composePostDetail(postDetailRow, userId);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 게시글 조회 시 예외 발생")
            void getPostDetail_NotFound_ThrowsException() {
                Long postId = 999L;
                given(postRepository.findByIdWithTechBlog(postId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> postQueryService.getPostDetail(new GetPostDetailQuery(postId, null)))
                        .isInstanceOf(GeneralException.class);

                verify(postRepository, times(1)).findByIdWithTechBlog(postId);
                verify(postReadModelCompositionService, never()).composePostDetail(any(), any());
            }
        }
    }

    @Nested
    @DisplayName("최근 게시글 조회")
    class GetRecentPosts {

        @Test
        @DisplayName("LATEST 정렬 시 최근 게시글 조회 후 조합한다")
        void getRecentPosts_Latest_Success() {
            Long lastPostId = null;
            int size = 20;
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(2L, "게시글 2", "카카오", LocalDateTime.now(), 50L, null, null, "thumb-2"),
                    postInfoRow(1L, "게시글 1", "네이버", LocalDateTime.now().minusDays(1), 100L, null, null, "thumb-1")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(2L, "게시글 2", "카카오", rawPosts.get(0).publishedAt(), 50L, List.of("Java"), null, "optimized-2"),
                    postInfoRow(1L, "게시글 1", "네이버", rawPosts.get(1).publishedAt(), 100L, List.of("Spring"), null, "optimized-1")
            );

            given(postRepository.findRecentPostsWithCursor(eq(lastPostId), any(PageRequest.class))).willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, null)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getRecentPosts(
                    new GetRecentPostsQuery(EPostSortType.LATEST, lastPostId, size, null)
            );

            assertThat(result.posts()).hasSize(2);
            assertThat(result.posts().get(0).keywords()).containsExactly("Java");
            assertThat(result.posts().get(1).keywords()).containsExactly("Spring");
            assertThat(result.hasNext()).isFalse();
            verify(postRepository, times(1)).findRecentPostsWithCursor(eq(lastPostId), any(PageRequest.class));
            verify(postRepository, never()).findPopularPostsWithCursor(any(), any());
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(rawPosts, null);
        }

        @Test
        @DisplayName("POPULAR 정렬 시 인기 게시글 repository 경로만 사용한다")
        void getRecentPosts_Popular_UsesPopularRepositoryPath() {
            Long lastPostId = 10L;
            int size = 20;
            Long userId = 7L;
            LocalDateTime now = LocalDateTime.now();
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(9L, "인기 게시글", "카카오", now, 300L, null, null, "thumb-9")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(9L, "인기 게시글", "카카오", now, 300L, List.of("AI"), true, "optimized-9")
            );

            given(postRepository.findPopularPostsWithCursor(eq(lastPostId), any(PageRequest.class))).willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, userId)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getRecentPosts(
                    new GetRecentPostsQuery(EPostSortType.POPULAR, lastPostId, size, userId)
            );

            assertThat(result.posts()).hasSize(1);
            assertThat(result.posts().get(0).isBookmarked()).isTrue();
            verify(postRepository, times(1)).findPopularPostsWithCursor(eq(lastPostId), any(PageRequest.class));
            verify(postRepository, never()).findRecentPostsWithCursor(any(), any());
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(rawPosts, userId);
        }

        @Test
        @DisplayName("size+1 조회 결과면 마지막 커서와 hasNext를 잘라서 계산한다")
        void getRecentPosts_SizePlusOneRows_SetsHasNextAndLastCursor() {
            Long lastPostId = null;
            int size = 2;
            LocalDateTime now = LocalDateTime.now();
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(3L, "게시글 3", "카카오", now, 300L, null, null, "thumb-3"),
                    postInfoRow(2L, "게시글 2", "네이버", now.minusHours(1), 200L, null, null, "thumb-2"),
                    postInfoRow(1L, "게시글 1", "라인", now.minusHours(2), 100L, null, null, "thumb-1")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(3L, "게시글 3", "카카오", now, 300L, List.of("Java"), null, "optimized-3"),
                    postInfoRow(2L, "게시글 2", "네이버", now.minusHours(1), 200L, List.of("Spring"), null, "optimized-2"),
                    postInfoRow(1L, "게시글 1", "라인", now.minusHours(2), 100L, List.of("Kotlin"), null, "optimized-1")
            );

            given(postRepository.findRecentPostsWithCursor(eq(lastPostId), any(PageRequest.class))).willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, null)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getRecentPosts(
                    new GetRecentPostsQuery(EPostSortType.LATEST, lastPostId, size, null)
            );

            assertThat(result.posts()).hasSize(2);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.lastPostId()).isEqualTo(2L);
            assertThat(result.lastViewCount()).isEqualTo(200L);
            assertThat(result.lastPublishedAt()).isEqualTo(now.minusHours(1));
        }

        @Test
        @DisplayName("빈 결과면 커서 값은 모두 null이고 hasNext는 false다")
        void getRecentPosts_EmptyRows_ReturnsNullCursors() {
            given(postRepository.findRecentPostsWithCursor(eq(null), any(PageRequest.class))).willReturn(List.of());
            given(postReadModelCompositionService.composePostInfoRows(List.of(), null)).willReturn(List.of());

            GetPostListResult result = postQueryService.getRecentPosts(
                    new GetRecentPostsQuery(EPostSortType.LATEST, null, 20, null)
            );

            assertThat(result.posts()).isEmpty();
            assertThat(result.lastPostId()).isNull();
            assertThat(result.lastViewCount()).isNull();
            assertThat(result.lastPublishedAt()).isNull();
            assertThat(result.hasNext()).isFalse();
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(List.of(), null);
        }
    }

    @Nested
    @DisplayName("최근 게시글 조회 V2")
    class GetRecentPostsV2 {

        @Test
        @DisplayName("LATEST 정렬 시 recent V2 repository 경로만 사용한다")
        void getRecentPostsV2_Latest_UsesLatestRepositoryPath() {
            LocalDateTime lastPublishedAt = LocalDateTime.now().minusDays(1);
            Long lastPostId = 20L;
            Long userId = 5L;
            LocalDateTime nextPublishedAt = lastPublishedAt.minusHours(1);
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(19L, "최신 게시글", "카카오", nextPublishedAt, 90L, null, null, "thumb-19")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(19L, "최신 게시글", "카카오", nextPublishedAt, 90L, List.of("Java"), false, "optimized-19")
            );

            given(postRepository.findRecentPostsWithCursorV2(eq(lastPublishedAt), eq(lastPostId), any(PageRequest.class)))
                    .willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, userId)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getRecentPostsV2(
                    new GetRecentPostsV2Query(EPostSortType.LATEST, null, lastPublishedAt, lastPostId, 20, userId)
            );

            assertThat(result.posts()).hasSize(1);
            assertThat(result.lastPublishedAt()).isEqualTo(nextPublishedAt);
            verify(postRepository, times(1)).findRecentPostsWithCursorV2(eq(lastPublishedAt), eq(lastPostId), any(PageRequest.class));
            verify(postRepository, never()).findPopularPostsWithCursorV2(any(), any(), any());
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(rawPosts, userId);
        }

        @Test
        @DisplayName("POPULAR 정렬 시 조합 후 size 기준으로 커서 응답을 만든다")
        void getRecentPostsV2_Popular_BuildsCursorFromComposedRows() {
            Integer lastViewCount = null;
            Long lastPostId = null;
            Long userId = 1L;
            int size = 1;
            LocalDateTime now = LocalDateTime.now();
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(1L, "인기 게시글 1", "카카오", now, 1000L, null, null, "thumb-1"),
                    postInfoRow(2L, "인기 게시글 2", "네이버", now.minusMinutes(1), 500L, null, null, "thumb-2")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(1L, "인기 게시글 1", "카카오", now, 1000L, List.of("Java"), false, "optimized-1"),
                    postInfoRow(2L, "인기 게시글 2", "네이버", now.minusMinutes(1), 500L, List.of("Spring"), true, "optimized-2")
            );

            given(postRepository.findPopularPostsWithCursorV2(eq(lastViewCount), eq(lastPostId), any(PageRequest.class)))
                    .willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, userId)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getRecentPostsV2(
                    new GetRecentPostsV2Query(EPostSortType.POPULAR, lastViewCount, null, lastPostId, size, userId)
            );

            assertThat(result.posts()).hasSize(1);
            assertThat(result.posts().get(0).isBookmarked()).isFalse();
            assertThat(result.lastPostId()).isEqualTo(1L);
            assertThat(result.lastViewCount()).isEqualTo(1000L);
            assertThat(result.hasNext()).isTrue();
            verify(postRepository, times(1)).findPopularPostsWithCursorV2(eq(lastViewCount), eq(lastPostId), any(PageRequest.class));
            verify(postRepository, never()).findRecentPostsWithCursorV2(any(), any(), any());
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(rawPosts, userId);
        }

        @Test
        @DisplayName("POPULAR 정렬 시 cursor 인자를 repository에 그대로 전달한다")
        void getRecentPostsV2_Popular_PropagatesCursorArguments() {
            Integer lastViewCount = 250;
            LocalDateTime lastPublishedAt = LocalDateTime.now().minusHours(2);
            Long lastPostId = 11L;
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(10L, "인기 게시글 10", "카카오", lastPublishedAt.minusMinutes(10), 200L, null, null, "thumb-10")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(10L, "인기 게시글 10", "카카오", lastPublishedAt.minusMinutes(10), 200L, List.of(), false, "optimized-10")
            );

            given(postRepository.findPopularPostsWithCursorV2(eq(lastViewCount), eq(lastPostId), any(PageRequest.class)))
                    .willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, null)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getRecentPostsV2(
                    new GetRecentPostsV2Query(EPostSortType.POPULAR, lastViewCount, lastPublishedAt, lastPostId, 20, null)
            );

            assertThat(result.posts()).hasSize(1);
            assertThat(result.lastPostId()).isEqualTo(10L);
            assertThat(result.lastViewCount()).isEqualTo(200L);
            verify(postRepository, times(1)).findPopularPostsWithCursorV2(eq(lastViewCount), eq(lastPostId), any(PageRequest.class));
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(rawPosts, null);
        }
    }

    @Nested
    @DisplayName("회사별 게시글 조회")
    class GetPostsByCompany {

        @Test
        @DisplayName("회사별 게시글 조회 후 로그인 사용자 북마크 결과를 유지한다")
        void getPostsByCompany_WithUserId_IncludesBookmarks() {
            String company = "카카오";
            Long userId = 1L;
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(1L, "카카오 게시글", company, LocalDateTime.now(), 50L, null, null, "thumb-1")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(1L, "카카오 게시글", company, rawPosts.get(0).publishedAt(), 50L, List.of("Java"), true, "optimized-1")
            );

            given(postRepository.findByCompanyWithCursor(eq(company), eq(null), any(PageRequest.class))).willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, userId)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getPostsByCompany(
                    new GetPostsByCompanyQuery(company, null, 20, userId)
            );

            assertThat(result.posts()).hasSize(1);
            assertThat(result.posts().get(0).company()).isEqualTo(company);
            assertThat(result.posts().get(0).isBookmarked()).isTrue();
            verify(postRepository, times(1)).findByCompanyWithCursor(eq(company), eq(null), any(PageRequest.class));
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(rawPosts, userId);
        }

        @Test
        @DisplayName("비로그인 조회는 null userId를 조합 서비스에 전달한다")
        void getPostsByCompany_WithoutUserId_DelegatesNullUserId() {
            String company = "카카오";
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(1L, "카카오 게시글", company, LocalDateTime.now(), 50L, null, null, "thumb-1")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(1L, "카카오 게시글", company, rawPosts.get(0).publishedAt(), 50L, List.of("Java"), null, "optimized-1")
            );

            given(postRepository.findByCompanyWithCursor(eq(company), eq(null), any(PageRequest.class))).willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, null)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getPostsByCompany(
                    new GetPostsByCompanyQuery(company, null, 20, null)
            );

            assertThat(result.posts()).hasSize(1);
            assertThat(result.posts().get(0).isBookmarked()).isNull();
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(rawPosts, null);
        }
    }

    @Nested
    @DisplayName("회사별 게시글 조회 V2")
    class GetPostsByCompanyV2 {

        @Test
        @DisplayName("companies가 null이면 전체 회사 조회 경로를 유지한다")
        void getPostsByCompanyV2_NullCompanies_UsesAllCompaniesPath() {
            LocalDateTime now = LocalDateTime.now();
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(2L, "네이버 게시글", "네이버", now, 100L, null, null, "thumb-2"),
                    postInfoRow(1L, "카카오 게시글", "카카오", now.minusHours(1), 50L, null, null, "thumb-1")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(2L, "네이버 게시글", "네이버", now, 100L, List.of("AI"), null, "optimized-2"),
                    postInfoRow(1L, "카카오 게시글", "카카오", now.minusHours(1), 50L, List.of("Java"), null, "optimized-1")
            );

            given(postRepository.findByCompanyNamesWithCursor(eq(null), eq(null), eq(null), any(PageRequest.class)))
                    .willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, null)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getPostsByCompanyV2(
                    new GetPostsByCompanyV2Query(null, null, null, 20, null)
            );

            assertThat(result.posts()).hasSize(2);
            assertThat(result.lastPostId()).isEqualTo(1L);
            verify(postRepository, times(1)).findByCompanyNamesWithCursor(eq(null), eq(null), eq(null), any(PageRequest.class));
        }

        @Test
        @DisplayName("조합된 결과의 마지막 게시글로 published cursor를 유지한다")
        void getPostsByCompanyV2_PreservesLastPublishedCursor() {
            List<String> companies = List.of("카카오", "네이버");
            LocalDateTime now = LocalDateTime.now();
            List<PostInfoRow> rawPosts = List.of(
                    postInfoRow(2L, "네이버 게시글", "네이버", now, 100L, null, null, "thumb-2"),
                    postInfoRow(1L, "카카오 게시글", "카카오", now.minusHours(1), 50L, null, null, "thumb-1")
            );
            List<PostInfoRow> composedPosts = List.of(
                    postInfoRow(2L, "네이버 게시글", "네이버", now, 100L, List.of("AI"), false, "optimized-2"),
                    postInfoRow(1L, "카카오 게시글", "카카오", now.minusHours(1), 50L, List.of("Java"), false, "optimized-1")
            );

            given(postRepository.findByCompanyNamesWithCursor(eq(companies), eq(null), eq(null), any(PageRequest.class)))
                    .willReturn(rawPosts);
            given(postReadModelCompositionService.composePostInfoRows(rawPosts, null)).willReturn(composedPosts);

            GetPostListResult result = postQueryService.getPostsByCompanyV2(
                    new GetPostsByCompanyV2Query(companies, null, null, 20, null)
            );

            assertThat(result.posts()).hasSize(2);
            assertThat(result.lastPostId()).isEqualTo(1L);
            assertThat(result.lastPublishedAt()).isEqualTo(now.minusHours(1));
            verify(postRepository, times(1)).findByCompanyNamesWithCursor(eq(companies), eq(null), eq(null), any(PageRequest.class));
            verify(postReadModelCompositionService, times(1)).composePostInfoRows(rawPosts, null);
        }
    }

    private PostInfoRow postInfoRow(
            Long id,
            String title,
            String company,
            LocalDateTime publishedAt,
            Long viewCount,
            List<String> keywords,
            Boolean isBookmarked,
            String thumbnailUrl
    ) {
        return PostInfoRow.builder()
                .id(id)
                .title(title)
                .shortSummary(title + " 요약")
                .company(company)
                .url("https://test.com/" + id)
                .logoUrl("https://test.com/logo-" + id + ".png")
                .thumbnailUrl(thumbnailUrl)
                .publishedAt(publishedAt)
                .viewCount(viewCount)
                .keywords(keywords)
                .isBookmarked(isBookmarked)
                .build();
    }
}
