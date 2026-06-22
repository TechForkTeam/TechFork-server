package com.techfork.post.integration;

import com.techfork.activity.bookmark.fixture.BookmarkFixture;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.fixture.TechBlogFixture;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.fixture.UserFixture;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.fixture.PostKeywordFixture;
import com.techfork.post.infrastructure.PostKeywordRepository;
import com.techfork.post.infrastructure.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostKeywordRepository postKeywordRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private TechBlog testTechBlog;
    private Post testPost1;
    private Post testPost2;
    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testTechBlog = TechBlogFixture.createTechBlog("테스트 회사", "https://test.com", "https://test.com/rss", null);
        techBlogRepository.save(testTechBlog);

        testPost1 = PostFixture.createPost(
                testTechBlog,
                "테스트 게시글 1",
                "<p>전체 내용 1</p>",
                "전체 내용 1",
                "요약 내용 1",
                "짧은 요약 1",
                "https://test.com/post/1/logo.png",
                "https://test.com/post/1/thumbnail.png",
                "https://test.com/post/1",
                LocalDateTime.of(2025, 1, 1, 10, 0)
        );
        testPost1 = postRepository.save(testPost1);
        postKeywordRepository.saveAll(List.of(
                PostKeywordFixture.postKeyword("Java", testPost1),
                PostKeywordFixture.postKeyword("Spring", testPost1)
        ));

        testPost2 = PostFixture.createPost(
                testTechBlog,
                "테스트 게시글 2",
                "<p>전체 내용 2</p>",
                "전체 내용 2",
                "요약 내용 2",
                "짧은 요약 2",
                "https://test.com/post/2/logo.png",
                "https://test.com/post/2/thumbnail.png",
                "https://test.com/post/2",
                LocalDateTime.of(2025, 1, 2, 10, 0)
        );
        testPost2 = postRepository.save(testPost2);
        postKeywordRepository.save(PostKeywordFixture.postKeyword("Kotlin", testPost2));
    }

    @BeforeEach
    void setUpUser() {
        testUser = UserFixture.socialUser("testSocialId", "test@example.com");
        testUser = userRepository.save(testUser);
        accessToken = jwtUtil.generateTokens(testUser.getId(), Role.USER).accessToken();
        bookmarkRepository.save(BookmarkFixture.createBookmark(testUser, testPost1, LocalDateTime.now()));
    }

    @AfterEach
    void tearDown() {
        bookmarkRepository.deleteAll();
        postKeywordRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/v1/posts/{postId}")
    class GetPostDetail {

        @Test
        @DisplayName("비로그인 시 게시글 상세 조회 성공 (isBookmarked는 null)")
        void withoutAuth_ReturnsPostDetail() throws Exception {
            mockMvc.perform(get("/api/v1/posts/{postId}", testPost1.getId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testPost1.getId()))
                    .andExpect(jsonPath("$.data.title").value("테스트 게시글 1"))
                    .andExpect(jsonPath("$.data.summary").exists())
                    .andExpect(jsonPath("$.data.company").value("테스트 회사"))
                    .andExpect(jsonPath("$.data.url").value("https://test.com/post/1"))
                    .andExpect(jsonPath("$.data.logoUrl").value("https://test.com/post/1/logo.png"))
                    .andExpect(jsonPath("$.data.publishedAt").exists())
                    .andExpect(jsonPath("$.data.viewCount").isNumber())
                    .andExpect(jsonPath("$.data.keywords").isArray())
                    .andExpect(jsonPath("$.data.keywords.length()").value(2))
                    .andExpect(jsonPath("$.data.isBookmarked").doesNotExist());
        }

        @Test
        @DisplayName("로그인 시 북마크한 게시글 상세 조회 (isBookmarked는 true)")
        void bookmarkedPostWithAuth_ReturnsBookmarkedDetail() throws Exception {
            mockMvc.perform(get("/api/v1/posts/{postId}", testPost1.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testPost1.getId()))
                    .andExpect(jsonPath("$.data.title").value("테스트 게시글 1"))
                    .andExpect(jsonPath("$.data.isBookmarked").value(true));
        }

        @Test
        @DisplayName("로그인 시 북마크하지 않은 게시글 상세 조회 (isBookmarked는 false)")
        void notBookmarkedPostWithAuth_ReturnsNotBookmarkedDetail() throws Exception {
            mockMvc.perform(get("/api/v1/posts/{postId}", testPost2.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testPost2.getId()))
                    .andExpect(jsonPath("$.data.title").value("테스트 게시글 2"))
                    .andExpect(jsonPath("$.data.isBookmarked").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 조회 시 404")
        void postNotFound_ReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/posts/{postId}", 99999L))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/companies")
    class GetCompanies {

        @Test
        @DisplayName("회사 목록 조회 성공")
        void companiesExist_ReturnsCompanies() throws Exception {
            mockMvc.perform(get("/api/v1/posts/companies"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.companies").isArray())
                    .andExpect(jsonPath("$.data.companies[0]").value("테스트 회사"))
                    .andExpect(jsonPath("$.data.companies.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/recent")
    class GetRecentPosts {

        @Test
        @DisplayName("비로그인 시 isBookmarked 미포함")
        void withoutAuth_ReturnsRecentPosts() throws Exception {
            mockMvc.perform(get("/api/v1/posts/recent")
                            .param("sortBy", "LATEST")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2))
                    .andExpect(jsonPath("$.data.lastPostId").exists())
                    .andExpect(jsonPath("$.data.lastViewCount").exists())
                    .andExpect(jsonPath("$.data.lastPublishedAt").exists())
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.posts[0].id").isNumber())
                    .andExpect(jsonPath("$.data.posts[0].title").isString())
                    .andExpect(jsonPath("$.data.posts[0].shortSummary").isString())
                    .andExpect(jsonPath("$.data.posts[0].company").isString())
                    .andExpect(jsonPath("$.data.posts[0].url").isString())
                    .andExpect(jsonPath("$.data.posts[0].logoUrl").exists())
                    .andExpect(jsonPath("$.data.posts[0].thumbnailUrl").exists())
                    .andExpect(jsonPath("$.data.posts[0].publishedAt").exists())
                    .andExpect(jsonPath("$.data.posts[0].viewCount").isNumber())
                    .andExpect(jsonPath("$.data.posts[0].keywords").isArray())
                    .andExpect(jsonPath("$.data.posts[0].isBookmarked").doesNotExist())
                    .andExpect(jsonPath("$.data.posts[1].isBookmarked").doesNotExist());
        }

        @Test
        @DisplayName("로그인 시 북마크 여부 포함 (testPost1=true, testPost2=false)")
        void withAuth_ReturnsRecentPostsWithBookmarkFlags() throws Exception {
            mockMvc.perform(get("/api/v1/posts/recent")
                            .param("sortBy", "LATEST")
                            .param("size", "20")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2))
                    .andExpect(jsonPath("$.data.posts[0].id").value(testPost2.getId()))
                    .andExpect(jsonPath("$.data.posts[0].shortSummary").value("짧은 요약 2"))
                    .andExpect(jsonPath("$.data.posts[0].isBookmarked").value(false))
                    .andExpect(jsonPath("$.data.posts[1].id").value(testPost1.getId()))
                    .andExpect(jsonPath("$.data.posts[1].shortSummary").value("짧은 요약 1"))
                    .andExpect(jsonPath("$.data.posts[1].isBookmarked").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/by-company")
    class GetPostsByCompany {

        @Test
        @DisplayName("비로그인 시 isBookmarked 미포함")
        void withoutAuth_ReturnsPostsByCompany() throws Exception {
            mockMvc.perform(get("/api/v1/posts/by-company")
                            .param("company", "테스트 회사")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2))
                    .andExpect(jsonPath("$.data.posts[0].isBookmarked").doesNotExist())
                    .andExpect(jsonPath("$.data.posts[1].isBookmarked").doesNotExist());
        }

        @Test
        @DisplayName("로그인 시 북마크 여부 포함 (testPost1=true, testPost2=false)")
        void withAuth_ReturnsPostsByCompanyWithBookmarkFlags() throws Exception {
            mockMvc.perform(get("/api/v1/posts/by-company")
                            .param("company", "테스트 회사")
                            .param("size", "20")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2))
                    .andExpect(jsonPath("$.data.posts[0].id").value(testPost2.getId()))
                    .andExpect(jsonPath("$.data.posts[0].shortSummary").value("짧은 요약 2"))
                    .andExpect(jsonPath("$.data.posts[0].isBookmarked").value(false))
                    .andExpect(jsonPath("$.data.posts[1].id").value(testPost1.getId()))
                    .andExpect(jsonPath("$.data.posts[1].shortSummary").value("짧은 요약 1"))
                    .andExpect(jsonPath("$.data.posts[1].isBookmarked").value(true));
        }
    }
}
