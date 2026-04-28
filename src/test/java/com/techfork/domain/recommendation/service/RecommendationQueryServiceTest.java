package com.techfork.domain.recommendation.service;

import com.techfork.domain.activity.repository.BookmarkRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.recommendation.converter.RecommendationConverter;
import com.techfork.domain.recommendation.dto.RecommendationListResponse;
import com.techfork.domain.recommendation.dto.RecommendedPostDto;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationQueryService 단위 테스트")
class RecommendationQueryServiceTest {

    @Mock
    private RecommendedPostRepository recommendedPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationConverter recommendationConverter;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private RecommendationQueryService recommendationQueryService;

    private User testUser;
    private TechBlog testTechBlog;
    private Post post1;
    private Post post2;
    private Post post3;
    private RecommendedPost recommendedPost1;
    private RecommendedPost recommendedPost2;
    private RecommendedPost recommendedPost3;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.createSocialUser(
                SocialType.KAKAO,
                "test-social-id",
                "test@example.com",
                "https://example.com/profile.jpg"
        );

        // 테스트 기술 블로그 생성
        testTechBlog = TechBlog.create(
                "테스트 회사",
                "https://test.com",
                "https://test.com/rss",
                "https://test.com/logo.png"
        );

        // 테스트 게시글 생성
        post1 = Post.builder()
                .title("게시글 1")
                .shortSummary("요약 1")
                .url("https://test.com/post1")
                .company("테스트 회사")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog)
                .build();

        post2 = Post.builder()
                .title("게시글 2")
                .shortSummary("요약 2")
                .url("https://test.com/post2")
                .company("테스트 회사")
                .publishedAt(LocalDateTime.now().minusDays(2))
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog)
                .build();

        post3 = Post.builder()
                .title("게시글 3")
                .shortSummary("요약 3")
                .url("https://test.com/post3")
                .company("테스트 회사")
                .publishedAt(LocalDateTime.now().minusDays(3))
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog)
                .build();

        // 테스트 추천 게시글 생성
        recommendedPost1 = RecommendedPost.create(testUser, post1, 0.9, 0.85, 1);
        recommendedPost2 = RecommendedPost.create(testUser, post2, 0.8, 0.75, 2);
        recommendedPost3 = RecommendedPost.create(testUser, post3, 0.7, 0.65, 3);
    }

    @Test
    @DisplayName("추천 게시글 목록을 조회한다")
    void getRecommendations() {
        // given
        Long userId = 1L;
        List<RecommendedPost> recommendedPosts = List.of(
                recommendedPost1, recommendedPost2, recommendedPost3
        );

        RecommendedPostDto dto1 = createRecommendedPostDto(1L, 101L, "게시글 1", "요약 1", null);
        RecommendedPostDto dto2 = createRecommendedPostDto(2L, 102L, "게시글 2", "요약 2", null);
        RecommendedPostDto dto3 = createRecommendedPostDto(3L, 103L, "게시글 3", "요약 3", null);

        RecommendationListResponse initialResponse = RecommendationListResponse.builder()
                .recommendations(List.of(dto1, dto2, dto3))
                .totalCount(3)
                .build();

        given(userRepository.getReferenceById(userId)).willReturn(testUser);
        given(recommendedPostRepository.findByUserOrderByRankAsc(testUser)).willReturn(recommendedPosts);
        given(recommendationConverter.toRecommendationListResponse(recommendedPosts)).willReturn(initialResponse);
        given(bookmarkRepository.findBookmarkedPostIds(eq(userId), any())).willReturn(List.of());

        // when
        RecommendationListResponse response = recommendationQueryService.getRecommendations(userId);

        // then
        assertThat(response.recommendations()).hasSize(3);
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.recommendations().get(0).title()).isEqualTo("게시글 1");
        assertThat(response.recommendations().get(0).isBookmarked()).isFalse();

        verify(userRepository).getReferenceById(userId);
        verify(recommendedPostRepository).findByUserOrderByRankAsc(testUser);
        verify(recommendationConverter).toRecommendationListResponse(recommendedPosts);
        verify(bookmarkRepository).findBookmarkedPostIds(eq(userId), any());
    }

    @Test
    @DisplayName("북마크한 게시글은 isBookmarked가 true로 설정된다")
    void getRecommendations_withBookmarkedPosts() {
        // given
        Long userId = 1L;
        List<RecommendedPost> recommendedPosts = List.of(
                recommendedPost1, recommendedPost2, recommendedPost3
        );

        RecommendedPostDto dto1 = createRecommendedPostDto(1L, 101L, "게시글 1", "요약 1", null);
        RecommendedPostDto dto2 = createRecommendedPostDto(2L, 102L, "게시글 2", "요약 2", null);
        RecommendedPostDto dto3 = createRecommendedPostDto(3L, 103L, "게시글 3", "요약 3", null);

        RecommendationListResponse initialResponse = RecommendationListResponse.builder()
                .recommendations(List.of(dto1, dto2, dto3))
                .totalCount(3)
                .build();

        // 101L, 103L 게시글은 북마크됨
        List<Long> bookmarkedPostIds = List.of(101L, 103L);

        given(userRepository.getReferenceById(userId)).willReturn(testUser);
        given(recommendedPostRepository.findByUserOrderByRankAsc(testUser)).willReturn(recommendedPosts);
        given(recommendationConverter.toRecommendationListResponse(recommendedPosts)).willReturn(initialResponse);
        given(bookmarkRepository.findBookmarkedPostIds(eq(userId), any())).willReturn(bookmarkedPostIds);

        // when
        RecommendationListResponse response = recommendationQueryService.getRecommendations(userId);

        // then
        assertThat(response.recommendations()).hasSize(3);
        assertThat(response.recommendations().get(0).postId()).isEqualTo(101L);
        assertThat(response.recommendations().get(0).isBookmarked()).isTrue();
        assertThat(response.recommendations().get(1).postId()).isEqualTo(102L);
        assertThat(response.recommendations().get(1).isBookmarked()).isFalse();
        assertThat(response.recommendations().get(2).postId()).isEqualTo(103L);
        assertThat(response.recommendations().get(2).isBookmarked()).isTrue();
    }

    @Test
    @DisplayName("추천 게시글이 없으면 빈 리스트를 반환한다")
    void getRecommendations_emptyList() {
        // given
        Long userId = 1L;
        List<RecommendedPost> emptyList = List.of();

        RecommendationListResponse emptyResponse = RecommendationListResponse.builder()
                .recommendations(List.of())
                .totalCount(0)
                .build();

        given(userRepository.getReferenceById(userId)).willReturn(testUser);
        given(recommendedPostRepository.findByUserOrderByRankAsc(testUser)).willReturn(emptyList);
        given(recommendationConverter.toRecommendationListResponse(emptyList)).willReturn(emptyResponse);

        // when
        RecommendationListResponse response = recommendationQueryService.getRecommendations(userId);

        // then
        assertThat(response.recommendations()).isEmpty();
        assertThat(response.totalCount()).isZero();

        verify(userRepository).getReferenceById(userId);
        verify(recommendedPostRepository).findByUserOrderByRankAsc(testUser);
        verify(recommendationConverter).toRecommendationListResponse(emptyList);
        verify(bookmarkRepository, never()).findBookmarkedPostIds(any(), any());
    }

    private RecommendedPostDto createRecommendedPostDto(
            Long id, Long postId, String title, String shortSummary, Boolean isBookmarked
    ) {
        return RecommendedPostDto.builder()
                .id(id)
                .postId(postId)
                .title(title)
                .shortSummary(shortSummary)
                .company("테스트 회사")
                .url("https://test.com/post" + postId)
                .logoUrl("https://test.com/logo.png")
                .thumbnailUrl("https://test.com/thumb.png")
                .viewCount(100L)
                .isBookmarked(isBookmarked)
                .publishedAt(LocalDateTime.now())
                .keywords(List.of("키워드1", "키워드2"))
                .similarityScore(0.9)
                .mmrScore(0.85)
                .rank(id.intValue())
                .recommendedAt(LocalDateTime.now())
                .build();
    }
}