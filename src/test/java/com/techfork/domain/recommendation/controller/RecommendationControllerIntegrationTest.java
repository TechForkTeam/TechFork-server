package com.techfork.domain.recommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.enums.Role;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RecommendationController 통합 테스트
 */
class RecommendationControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private RecommendedPostRepository recommendedPostRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String accessToken;
    private TechBlog testBlog;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.createSocialUser(
                SocialType.KAKAO,
                "testSocialId",
                "test@example.com",
                "profile.jpg"
        );
        testUser.updateUser("테스트유저", "test@example.com", "백엔드 개발자입니다.");
        testUser = userRepository.save(testUser);

        // JWT 토큰 생성
        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        accessToken = tokens.accessToken();

        // 테스트 블로그 생성
        testBlog = TechBlog.builder()
                .companyName("테스트회사")
                .blogUrl("https://test.com")
                .rssUrl("https://test.com/rss")
                .logoUrl("https://test.com/logo.png")
                .build();
        testBlog = techBlogRepository.save(testBlog);

        // 테스트 게시글 3개 생성
        testPost1 = Post.builder()
                .title("추천 게시글 1")
                .fullContent("게시글 1의 전체 내용입니다.")
                .plainContent("게시글 1의 내용")
                .summary("게시글 1의 요약")
                .shortSummary("게시글 1의 짧은 요약")
                .company("테스트회사")
                .logoUrl("https://test.com/logo.png")
                .thumbnailUrl("https://test.com/thumb1.png")
                .url("https://test.com/post/1")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost1 = postRepository.save(testPost1);

        testPost2 = Post.builder()
                .title("추천 게시글 2")
                .fullContent("게시글 2의 전체 내용입니다.")
                .plainContent("게시글 2의 내용")
                .summary("게시글 2의 요약")
                .shortSummary("게시글 2의 짧은 요약")
                .company("테스트회사")
                .logoUrl("https://test.com/logo.png")
                .thumbnailUrl("https://test.com/thumb2.png")
                .url("https://test.com/post/2")
                .publishedAt(LocalDateTime.now().minusDays(2))
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost2 = postRepository.save(testPost2);

        testPost3 = Post.builder()
                .title("추천 게시글 3")
                .fullContent("게시글 3의 전체 내용입니다.")
                .plainContent("게시글 3의 내용")
                .summary("게시글 3의 요약")
                .shortSummary("게시글 3의 짧은 요약")
                .company("테스트회사")
                .logoUrl("https://test.com/logo.png")
                .thumbnailUrl("https://test.com/thumb3.png")
                .url("https://test.com/post/3")
                .publishedAt(LocalDateTime.now().minusDays(3))
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost3 = postRepository.save(testPost3);
    }

    @AfterEach
    void tearDown() {
        recommendedPostRepository.deleteAll();
        bookmarkRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ===== 추천 게시글 조회 테스트 =====

    @Test
    @DisplayName("추천 게시글 목록 조회 성공 - 빈 목록")
    void getRecommendations_Success_Empty() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.data.recommendations").isArray())
                .andExpect(jsonPath("$.data.recommendations").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("추천 게시글 목록 조회 성공 - 여러 개")
    void getRecommendations_Success_Multiple() throws Exception {
        // Given - 추천 게시글 3개 생성
        RecommendedPost rec1 = RecommendedPost.create(testUser, testPost1, 0.9, 0.85, 1);
        RecommendedPost rec2 = RecommendedPost.create(testUser, testPost2, 0.8, 0.75, 2);
        RecommendedPost rec3 = RecommendedPost.create(testUser, testPost3, 0.7, 0.65, 3);
        recommendedPostRepository.saveAll(List.of(rec1, rec2, rec3));

        // When & Then
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.recommendations").isArray())
                .andExpect(jsonPath("$.data.recommendations.length()").value(3))
                // 첫 번째 추천 (rank 1)
                .andExpect(jsonPath("$.data.recommendations[0].postId").value(testPost1.getId()))
                .andExpect(jsonPath("$.data.recommendations[0].title").value("추천 게시글 1"))
                .andExpect(jsonPath("$.data.recommendations[0].shortSummary").value("게시글 1의 짧은 요약"))
                .andExpect(jsonPath("$.data.recommendations[0].company").value("테스트회사"))
                .andExpect(jsonPath("$.data.recommendations[0].url").value("https://test.com/post/1"))
                .andExpect(jsonPath("$.data.recommendations[0].logoUrl").value("https://test.com/logo.png"))
                .andExpect(jsonPath("$.data.recommendations[0].thumbnailUrl").value("https://test.com/thumb1.png"))
                .andExpect(jsonPath("$.data.recommendations[0].viewCount").value(0))
                .andExpect(jsonPath("$.data.recommendations[0].isBookmarked").value(false))
                .andExpect(jsonPath("$.data.recommendations[0].publishedAt").exists())
                .andExpect(jsonPath("$.data.recommendations[0].keywords").isArray())
                .andExpect(jsonPath("$.data.recommendations[0].similarityScore").value(0.9))
                .andExpect(jsonPath("$.data.recommendations[0].mmrScore").value(0.85))
                .andExpect(jsonPath("$.data.recommendations[0].rank").value(1))
                // 두 번째 추천 (rank 2)
                .andExpect(jsonPath("$.data.recommendations[1].rank").value(2))
                // 세 번째 추천 (rank 3)
                .andExpect(jsonPath("$.data.recommendations[2].rank").value(3));
    }

    @Test
    @DisplayName("추천 게시글 목록 조회 성공 - 랭킹 순으로 정렬")
    void getRecommendations_Success_OrderedByRank() throws Exception {
        // Given - 의도적으로 순서를 섞어서 저장
        RecommendedPost rec3 = RecommendedPost.create(testUser, testPost3, 0.7, 0.65, 3);
        RecommendedPost rec1 = RecommendedPost.create(testUser, testPost1, 0.9, 0.85, 1);
        RecommendedPost rec2 = RecommendedPost.create(testUser, testPost2, 0.8, 0.75, 2);
        recommendedPostRepository.saveAll(List.of(rec3, rec1, rec2));

        // When & Then - rank 순서대로 조회되어야 함
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.data.recommendations[0].postId").value(testPost1.getId()))
                .andExpect(jsonPath("$.data.recommendations[1].rank").value(2))
                .andExpect(jsonPath("$.data.recommendations[1].postId").value(testPost2.getId()))
                .andExpect(jsonPath("$.data.recommendations[2].rank").value(3))
                .andExpect(jsonPath("$.data.recommendations[2].postId").value(testPost3.getId()));
    }

    // ===== 통합 시나리오 테스트 =====

    @Test
    @DisplayName("통합 시나리오 - 추천 조회 후 북마크 추가 후 다시 조회")
    void integrationScenario_GetRecommendations_AddBookmark_GetAgain() throws Exception {
        // 1. 추천 게시글 생성
        RecommendedPost rec1 = RecommendedPost.create(testUser, testPost1, 0.9, 0.85, 1);
        recommendedPostRepository.save(rec1);

        // 2. 추천 조회 - 북마크 안됨
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations[0].isBookmarked").value(false));

        // 3. 북마크 추가
        Bookmark bookmark = Bookmark.create(testUser, testPost1, LocalDateTime.now());
        bookmarkRepository.save(bookmark);

        // 4. 다시 조회 - 북마크됨
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations[0].isBookmarked").value(true))
                .andExpect(jsonPath("$.data.recommendations[0].shortSummary").value("게시글 1의 짧은 요약"));
    }
}