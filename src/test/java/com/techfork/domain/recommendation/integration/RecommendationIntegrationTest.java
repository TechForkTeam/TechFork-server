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
import com.techfork.useraccount.domain.enums.SocialType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
        testUser = User.createSocialUser(
                SocialType.KAKAO,
                "testSocialId",
                "test@example.com",
                "profile.jpg"
        );
        testUser.updateUser("테스트유저", "test@example.com", "백엔드 개발자입니다.");
        testUser = userRepository.save(testUser);

        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        accessToken = tokens.accessToken();

        testBlog = TechBlog.builder()
                .companyName("테스트회사")
                .blogUrl("https://test.com")
                .rssUrl("https://test.com/rss")
                .logoUrl("https://test.com/logo.png")
                .build();
        testBlog = techBlogRepository.save(testBlog);

        testPost1 = postRepository.save(createPost(
                "추천 게시글 1",
                "게시글 1의 전체 내용입니다.",
                "게시글 1의 내용",
                "게시글 1의 요약",
                "게시글 1의 짧은 요약",
                "https://test.com/thumb1.png",
                "https://test.com/post/1",
                LocalDateTime.now().minusDays(1)
        ));
        testPost2 = postRepository.save(createPost(
                "추천 게시글 2",
                "게시글 2의 전체 내용입니다.",
                "게시글 2의 내용",
                "게시글 2의 요약",
                "게시글 2의 짧은 요약",
                "https://test.com/thumb2.png",
                "https://test.com/post/2",
                LocalDateTime.now().minusDays(2)
        ));
        testPost3 = postRepository.save(createPost(
                "추천 게시글 3",
                "게시글 3의 전체 내용입니다.",
                "게시글 3의 내용",
                "게시글 3의 요약",
                "게시글 3의 짧은 요약",
                "https://test.com/thumb3.png",
                "https://test.com/post/3",
                LocalDateTime.now().minusDays(3)
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
        void success_empty() throws Exception {
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
        void success_multiple() throws Exception {
            RecommendedPost rec1 = RecommendedPost.create(testUser, testPost1, 0.9, 0.85, 1);
            RecommendedPost rec2 = RecommendedPost.create(testUser, testPost2, 0.8, 0.75, 2);
            RecommendedPost rec3 = RecommendedPost.create(testUser, testPost3, 0.7, 0.65, 3);
            recommendedPostRepository.saveAll(List.of(rec1, rec2, rec3));

            mockMvc.perform(get("/api/v1/recommendations")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
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
        void success_orderedByRank() throws Exception {
            RecommendedPost rec3 = RecommendedPost.create(testUser, testPost3, 0.7, 0.65, 3);
            RecommendedPost rec1 = RecommendedPost.create(testUser, testPost1, 0.9, 0.85, 1);
            RecommendedPost rec2 = RecommendedPost.create(testUser, testPost2, 0.8, 0.75, 2);
            recommendedPostRepository.saveAll(List.of(rec3, rec1, rec2));

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
    }

    @Nested
    @DisplayName("추천과 북마크 통합 시나리오")
    class RecommendationBookmarkScenario {

        @Test
        @DisplayName("추천 조회 후 북마크 추가 후 다시 조회")
        void getRecommendations_addBookmark_getAgain() throws Exception {
            RecommendedPost rec1 = RecommendedPost.create(testUser, testPost1, 0.9, 0.85, 1);
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

    private Post createPost(
            String title,
            String fullContent,
            String plainContent,
            String summary,
            String shortSummary,
            String thumbnailUrl,
            String url,
            LocalDateTime publishedAt
    ) {
        return Post.builder()
                .title(title)
                .fullContent(fullContent)
                .plainContent(plainContent)
                .summary(summary)
                .shortSummary(shortSummary)
                .company("테스트회사")
                .logoUrl("https://test.com/logo.png")
                .thumbnailUrl(thumbnailUrl)
                .url(url)
                .publishedAt(publishedAt)
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
    }
}
