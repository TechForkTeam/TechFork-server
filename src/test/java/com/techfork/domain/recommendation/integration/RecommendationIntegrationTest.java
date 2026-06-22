package com.techfork.domain.recommendation.integration;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.domain.recommendation.entity.RecommendedPost;
import com.techfork.domain.recommendation.repository.RecommendedPostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.techfork.useraccount.fixture.UserFixture.activeUser;
import static com.techfork.post.fixture.PostFixture.DEFAULT_PUBLISHED_AT;
import static com.techfork.post.fixture.PostFixture.createPost;
import static com.techfork.domain.recommendation.fixture.RecommendedPostFixture.recommendedPost;
import static com.techfork.domain.source.fixture.TechBlogFixture.createTechBlog;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 추천 통합 테스트
 */
class RecommendationIntegrationTest extends IntegrationTestBase {

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
    private JwtUtil jwtUtil;

    private User testUser;
    private String accessToken;
    private TechBlog testBlog;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(activeUser("testSocialId", "test@example.com"));

        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        accessToken = tokens.accessToken();

        testBlog = techBlogRepository.save(createTechBlog("테스트회사", "https://test.com"));

        testPost1 = postRepository.save(createPost(
                testBlog,
                "추천 게시글 1",
                "게시글 1의 전체 내용입니다.",
                "게시글 1의 내용",
                "게시글 1의 요약",
                "게시글 1의 짧은 요약",
                "https://test.com/thumb1.png",
                "https://test.com/post/1",
                DEFAULT_PUBLISHED_AT.minusDays(1)
        ));
        testPost2 = postRepository.save(createPost(
                testBlog,
                "추천 게시글 2",
                "게시글 2의 전체 내용입니다.",
                "게시글 2의 내용",
                "게시글 2의 요약",
                "게시글 2의 짧은 요약",
                "https://test.com/thumb2.png",
                "https://test.com/post/2",
                DEFAULT_PUBLISHED_AT.minusDays(2)
        ));
        testPost3 = postRepository.save(createPost(
                testBlog,
                "추천 게시글 3",
                "게시글 3의 전체 내용입니다.",
                "게시글 3의 내용",
                "게시글 3의 요약",
                "게시글 3의 짧은 요약",
                "https://test.com/thumb3.png",
                "https://test.com/post/3",
                DEFAULT_PUBLISHED_AT.minusDays(3)
        ));
    }

    @AfterEach
    void tearDown() {
        recommendedPostRepository.deleteAll();
        bookmarkRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("추천 게시글 조회")
    class GetRecommendations {

        @Test
        @DisplayName("추천 게시글 목록 조회 성공 - 빈 목록")
        void getRecommendations_WhenEmpty_ReturnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/v1/recommendations")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.recommendations").isArray())
                    .andExpect(jsonPath("$.data.recommendations").isEmpty())
                    .andExpect(jsonPath("$.data.totalCount").value(0));
        }

        @Test
        @DisplayName("추천 게시글 목록 조회 성공 - 여러 개")
        void getRecommendations_WhenMultipleRecommendations_ReturnsRecommendationList() throws Exception {
            RecommendedPost rec1 = recommendedPost(testUser, testPost1, 1);
            RecommendedPost rec2 = recommendedPost(testUser, testPost2, 2);
            RecommendedPost rec3 = recommendedPost(testUser, testPost3, 3);
            recommendedPostRepository.saveAll(List.of(rec1, rec2, rec3));

            mockMvc.perform(get("/api/v1/recommendations")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.totalCount").value(3))
                    .andExpect(jsonPath("$.data.recommendations").isArray())
                    .andExpect(jsonPath("$.data.recommendations.length()").value(3))
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
                    .andExpect(jsonPath("$.data.recommendations[1].rank").value(2))
                    .andExpect(jsonPath("$.data.recommendations[2].rank").value(3));
        }

        @Test
        @DisplayName("추천 게시글 목록 조회 성공 - 랭킹 순으로 정렬")
        void getRecommendations_WhenSavedOutOfOrder_ReturnsOrderedByRank() throws Exception {
            RecommendedPost rec3 = recommendedPost(testUser, testPost3, 3);
            RecommendedPost rec1 = recommendedPost(testUser, testPost1, 1);
            RecommendedPost rec2 = recommendedPost(testUser, testPost2, 2);
            recommendedPostRepository.saveAll(List.of(rec3, rec1, rec2));

            mockMvc.perform(get("/api/v1/recommendations")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recommendations[0].rank").value(1))
                    .andExpect(jsonPath("$.data.recommendations[0].postId").value(testPost1.getId()))
                    .andExpect(jsonPath("$.data.recommendations[1].rank").value(2))
                    .andExpect(jsonPath("$.data.recommendations[1].postId").value(testPost2.getId()))
                    .andExpect(jsonPath("$.data.recommendations[2].rank").value(3))
                    .andExpect(jsonPath("$.data.recommendations[2].postId").value(testPost3.getId()));
        }
    }

    @Nested
    @DisplayName("추천과 북마크 통합 시나리오")
    class RecommendationBookmarkScenario {

        @Test
        @DisplayName("추천 조회 후 북마크 추가 후 다시 조회")
        void getRecommendations_WhenBookmarkAdded_ReturnsUpdatedBookmarkStatus() throws Exception {
            RecommendedPost rec1 = recommendedPost(testUser, testPost1, 1);
            recommendedPostRepository.save(rec1);

            mockMvc.perform(get("/api/v1/recommendations")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recommendations[0].isBookmarked").value(false));

            Bookmark bookmark = Bookmark.create(testUser, testPost1, LocalDateTime.now());
            bookmarkRepository.save(bookmark);

            mockMvc.perform(get("/api/v1/recommendations")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recommendations[0].isBookmarked").value(true))
                    .andExpect(jsonPath("$.data.recommendations[0].shortSummary").value("게시글 1의 짧은 요약"));
        }
    }

}
