package com.techfork.domain.post.controller;

import com.techfork.domain.activity.entity.ScrabPost;
import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.global.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * PostController 통합 테스트
 * - 모든 레이어(Controller, Service, Repository) 통합 테스트
 * - MockMvc로 HTTP 요청/응답 테스트
 */
class PostControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostKeywordRepository postKeywordRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private ScrabPostRepository scrabPostRepository;

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
        // Given: 실제 DB에 테스트 데이터 저장
        testTechBlog = TechBlog.builder()
                .companyName("테스트 회사")
                .blogUrl("https://test.com")
                .rssUrl("https://test.com/rss")
                .build();
        techBlogRepository.save(testTechBlog);

        testPost1 = Post.builder()
                .title("테스트 게시글 1")
                .fullContent("<p>전체 내용 1</p>")
                .plainContent("전체 내용 1")
                .summary("요약 내용 1")
                .shortSummary("짧은 요약 1")
                .company("테스트 회사")
                .url("https://test.com/post/1")
                .logoUrl("https://test.com/post/1/logo.png")
                .thumbnailUrl("https://test.com/post/1/thumbnail.png")
                .publishedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog)
                .build();
        testPost1 = postRepository.save(testPost1);

        // 키워드 추가
        PostKeyword keyword1 = PostKeyword.create("Java", testPost1);
        PostKeyword keyword2 = PostKeyword.create("Spring", testPost1);
        postKeywordRepository.saveAll(List.of(keyword1, keyword2));

        testPost2 = Post.builder()
                .title("테스트 게시글 2")
                .fullContent("<p>전체 내용 2</p>")
                .plainContent("전체 내용 2")
                .summary("요약 내용 2")
                .shortSummary("짧은 요약 2")
                .company("테스트 회사")
                .url("https://test.com/post/2")
                .logoUrl("https://test.com/post/2/logo.png")
                .thumbnailUrl("https://test.com/post/2/thumbnail.png")
                .publishedAt(LocalDateTime.of(2025, 1, 2, 10, 0))
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog)
                .build();
        testPost2 = postRepository.save(testPost2);

        PostKeyword keyword3 = PostKeyword.create("Kotlin", testPost2);
        postKeywordRepository.save(keyword3);
    }

    @BeforeEach
    void setUpUser() {
        // 테스트 사용자 생성
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
        testUser = userRepository.save(testUser);

        // JWT 토큰 생성
        accessToken = jwtUtil.generateTokens(testUser.getId(), Role.USER).accessToken();

        // testUser가 testPost1을 북마크
        ScrabPost scrabPost = ScrabPost.create(testUser, testPost1, LocalDateTime.now());
        scrabPostRepository.save(scrabPost);
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리 (외래키 제약조건 순서 고려)
        scrabPostRepository.deleteAll();
        postKeywordRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 비로그인 시 게시글 상세 조회 성공 (isBookmarked는 null)")
    void getPostDetail_WithoutAuth_Success() throws Exception {
        // When & Then: 실제 DB에 저장된 데이터로 HTTP 요청 테스트
        mockMvc.perform(get("/api/v1/posts/{postId}", testPost1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                // PostDetailDto의 모든 필드 검증
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
    @DisplayName("GET /api/v1/posts/{postId} - 로그인 시 북마크한 게시글 상세 조회 (isBookmarked는 true)")
    void getPostDetail_WithAuth_BookmarkedPost_Success() throws Exception {
        // When & Then: 로그인한 사용자가 북마크한 게시글 조회
        mockMvc.perform(get("/api/v1/posts/{postId}", testPost1.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testPost1.getId()))
                .andExpect(jsonPath("$.data.title").value("테스트 게시글 1"))
                .andExpect(jsonPath("$.data.isBookmarked").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 로그인 시 북마크하지 않은 게시글 상세 조회 (isBookmarked는 false)")
    void getPostDetail_WithAuth_NotBookmarkedPost_Success() throws Exception {
        // When & Then: 로그인한 사용자가 북마크하지 않은 게시글 조회
        mockMvc.perform(get("/api/v1/posts/{postId}", testPost2.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testPost2.getId()))
                .andExpect(jsonPath("$.data.title").value("테스트 게시글 2"))
                .andExpect(jsonPath("$.data.isBookmarked").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 존재하지 않는 게시글 조회 시 404")
    void getPostDetail_NotFound() throws Exception {
        // When & Then: 존재하지 않는 게시글 조회
        mockMvc.perform(get("/api/v1/posts/{postId}", 99999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/posts/companies - 회사 목록 조회 성공")
    void getCompanies_Success() throws Exception {
        // When & Then: 실제 DB에서 회사 목록 조회
        mockMvc.perform(get("/api/v1/posts/companies"))
                .andDo(print())
                .andExpect(status().isOk())
                // CompanyListResponse의 모든 필드 검증 (V1)
                .andExpect(jsonPath("$.data.companies").isArray())
                .andExpect(jsonPath("$.data.companies[0]").value("테스트 회사"))
                .andExpect(jsonPath("$.data.companies.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/posts/recent - 비로그인 시 isBookmarked 미포함")
    void getRecentPosts_WithoutAuth() throws Exception {
        // When & Then: 비로그인 상태에서 게시글 조회
        mockMvc.perform(get("/api/v1/posts/recent")
                        .param("sortBy", "LATEST")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                // PostListResponse의 모든 필드 검증
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                .andExpect(jsonPath("$.data.lastPostId").exists())
                .andExpect(jsonPath("$.data.lastViewCount").exists())
                .andExpect(jsonPath("$.data.lastPublishedAt").exists())
                .andExpect(jsonPath("$.data.hasNext").value(false))
                // PostInfoDto의 모든 필드 검증 (첫 번째 항목만)
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
                // isBookmarked는 비로그인이므로 없어야 함
                .andExpect(jsonPath("$.data.posts[0].isBookmarked").doesNotExist())
                .andExpect(jsonPath("$.data.posts[1].isBookmarked").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/posts/recent - 로그인 시 북마크 여부 포함 (testPost1=true, testPost2=false)")
    void getRecentPosts_WithAuth() throws Exception {
        // When & Then: 로그인 상태에서 게시글 조회
        mockMvc.perform(get("/api/v1/posts/recent")
                        .param("sortBy", "LATEST")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                // testPost2가 최신이므로 먼저 오고 false여야 함
                .andExpect(jsonPath("$.data.posts[0].id").value(testPost2.getId()))
                .andExpect(jsonPath("$.data.posts[0].shortSummary").value("짧은 요약 2"))
                .andExpect(jsonPath("$.data.posts[0].isBookmarked").value(false))
                // testPost1은 북마크되어 있으므로 true여야 함
                .andExpect(jsonPath("$.data.posts[1].id").value(testPost1.getId()))
                .andExpect(jsonPath("$.data.posts[1].shortSummary").value("짧은 요약 1"))
                .andExpect(jsonPath("$.data.posts[1].isBookmarked").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/posts/by-company - 비로그인 시 isBookmarked 미포함")
    void getPostsByCompany_WithoutAuth() throws Exception {
        // When & Then: 비로그인 상태에서 회사별 게시글 조회
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
    @DisplayName("GET /api/v1/posts/by-company - 로그인 시 북마크 여부 포함 (testPost1=true, testPost2=false)")
    void getPostsByCompany_WithAuth() throws Exception {
        // When & Then: 특정 회사 게시글 조회
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
