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
import com.techfork.global.common.MySqlRedisIntegrationTestBase;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.infrastructure.PostRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostV2IntegrationTest extends MySqlRedisIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private TechBlog testTechBlog1;
    private TechBlog testTechBlog2;
    private Post todayPost;
    private Post oldPost;
    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = UserFixture.socialUser("testSocialId", "test@example.com");
        testUser = userRepository.save(testUser);
        accessToken = jwtUtil.generateTokens(testUser.getId(), Role.USER).accessToken();

        testTechBlog1 = TechBlogFixture.createTechBlog("카카오", "https://kakao.com");
        testTechBlog1 = techBlogRepository.save(testTechBlog1);

        testTechBlog2 = TechBlogFixture.createTechBlog("네이버", "https://naver.com");
        testTechBlog2 = techBlogRepository.save(testTechBlog2);

        todayPost = PostFixture.createPost(
                testTechBlog1,
                "오늘의 게시글",
                "<p>오늘 내용</p>",
                "오늘 내용",
                "오늘 요약",
                "오늘 짧은 요약",
                "https://kakao.com/thumbnail.png",
                "https://kakao.com/post/today",
                LocalDate.now().atStartOfDay()
        );
        postRepository.save(todayPost);

        oldPost = PostFixture.createPost(
                testTechBlog2,
                "어제의 게시글",
                "<p>어제 내용</p>",
                "어제 내용",
                "어제 요약",
                "어제 짧은 요약",
                "https://naver.com/thumbnail.png",
                "https://naver.com/post/old",
                LocalDate.now().minusDays(1).atStartOfDay()
        );
        postRepository.save(oldPost);

        bookmarkRepository.save(BookmarkFixture.createBookmark(testUser, todayPost, LocalDateTime.now()));
    }

    @AfterEach
    void tearDown() {
        bookmarkRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/v2/posts/companies")
    class GetCompanies {

        @Test
        @DisplayName("회사 목록 상세 조회 성공")
        void companiesExist_ReturnsCompanies() throws Exception {
            mockMvc.perform(get("/api/v2/posts/companies"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalNumber").value(2))
                    .andExpect(jsonPath("$.data.companies").isArray())
                    .andExpect(jsonPath("$.data.companies.length()").value(2))
                    .andExpect(jsonPath("$.data.companies[0].company").value("카카오"))
                    .andExpect(jsonPath("$.data.companies[0].hasNewPost").value(true))
                    .andExpect(jsonPath("$.data.companies[0].logoUrl").value("https://kakao.com/logo.png"));
        }

        @Test
        @DisplayName("게시글이 없는 경우 빈 배열 반환")
        void noPosts_ReturnsEmptyCompanies() throws Exception {
            bookmarkRepository.deleteAll();
            postRepository.deleteAll();

            mockMvc.perform(get("/api/v2/posts/companies"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalNumber").value(0))
                    .andExpect(jsonPath("$.data.companies").isArray())
                    .andExpect(jsonPath("$.data.companies.length()").value(0));
        }

        @Test
        @DisplayName("발행일 기준 정렬 확인")
        void postsExist_ReturnsCompaniesSortedByLatestPublishedAt() throws Exception {
            Post newerPost = PostFixture.createPost(
                    testTechBlog2,
                    "최신 네이버 게시글",
                    "<p>최신 내용</p>",
                    "최신 내용",
                    null,
                    null,
                    null,
                    "https://naver.com/post/newest",
                    LocalDateTime.now()
            );
            postRepository.save(newerPost);

            mockMvc.perform(get("/api/v2/posts/companies"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.companies[0].company").value("네이버"))
                    .andExpect(jsonPath("$.data.companies[1].company").value("카카오"));
        }
    }

    @Nested
    @DisplayName("GET /api/v2/posts/by-company")
    class GetPostsByCompany {

        @Test
        @DisplayName("여러 회사의 게시글 조회 성공")
        void multipleCompanies_ReturnsPosts() throws Exception {
            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("companies", "카카오", "네이버")
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
                    .andExpect(jsonPath("$.data.posts[0].company").value("카카오"))
                    .andExpect(jsonPath("$.data.posts[0].url").isString())
                    .andExpect(jsonPath("$.data.posts[0].logoUrl").exists())
                    .andExpect(jsonPath("$.data.posts[0].thumbnailUrl").exists())
                    .andExpect(jsonPath("$.data.posts[0].publishedAt").exists())
                    .andExpect(jsonPath("$.data.posts[0].viewCount").isNumber())
                    .andExpect(jsonPath("$.data.posts[0].keywords").isArray());
        }

        @Test
        @DisplayName("companies 없으면 전체 조회")
        void noCompaniesParam_ReturnsAllPosts() throws Exception {
            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2));
        }

        @Test
        @DisplayName("단일 회사만 조회")
        void singleCompany_ReturnsPosts() throws Exception {
            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("companies", "카카오")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(1))
                    .andExpect(jsonPath("$.data.posts[0].company").value("카카오"))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("커서 페이징")
        void cursorProvided_ReturnsNextPage() throws Exception {
            for (int i = 1; i <= 5; i++) {
                postRepository.save(PostFixture.createPost(
                        testTechBlog1,
                        "카카오 게시글 " + i,
                        "<p>내용 " + i + "</p>",
                        "내용 " + i,
                        null,
                        null,
                        "https://kakao.com/thumbnail.png",
                        "https://kakao.com/post/" + i,
                        LocalDateTime.now().minusHours(i)
                ));
            }

            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("companies", "카카오")
                            .param("size", "3"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts.length()").value(3))
                    .andExpect(jsonPath("$.data.hasNext").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 회사 조회 시 빈 배열")
        void nonExistentCompany_ReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("companies", "존재하지않는회사")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(0))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("발행일 기준 정렬 확인")
        void postsExist_ReturnsSortedByPublishedAt() throws Exception {
            postRepository.save(PostFixture.createPost(
                    testTechBlog1,
                    "최신 게시글",
                    "<p>최신</p>",
                    "최신",
                    null,
                    null,
                    "https://kakao.com/thumbnail.png",
                    "https://kakao.com/post/recent",
                    LocalDateTime.now()
            ));

            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("companies", "카카오")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts[0].title").value("최신 게시글"))
                    .andExpect(jsonPath("$.data.posts[1].title").value("오늘의 게시글"));
        }

        @Test
        @DisplayName("비로그인 시 isBookmarked 미포함")
        void withoutAuth_ReturnsPostsByCompany() throws Exception {
            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("companies", "카카오", "네이버")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2))
                    .andExpect(jsonPath("$.data.posts[0].isBookmarked").doesNotExist())
                    .andExpect(jsonPath("$.data.posts[1].isBookmarked").doesNotExist());
        }

        @Test
        @DisplayName("로그인 시 북마크 여부 포함 (todayPost=true, oldPost=false)")
        void withAuth_ReturnsPostsByCompanyWithBookmarkFlags() throws Exception {
            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("companies", "카카오", "네이버")
                            .param("size", "20")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2))
                    .andExpect(jsonPath("$.data.posts[0].id").value(todayPost.getId()))
                    .andExpect(jsonPath("$.data.posts[0].isBookmarked").value(true))
                    .andExpect(jsonPath("$.data.posts[1].id").value(oldPost.getId()))
                    .andExpect(jsonPath("$.data.posts[1].isBookmarked").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v2/posts/recent")
    class GetRecentPosts {

        @Test
        @DisplayName("LATEST 정렬로 최근 게시글 조회")
        void latestSort_ReturnsRecentPosts() throws Exception {
            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("sortBy", "LATEST")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2))
                    .andExpect(jsonPath("$.data.lastPostId").exists())
                    .andExpect(jsonPath("$.data.lastPublishedAt").exists())
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.posts[0].id").isNumber())
                    .andExpect(jsonPath("$.data.posts[0].title").value("오늘의 게시글"))
                    .andExpect(jsonPath("$.data.posts[0].shortSummary").value("오늘 짧은 요약"))
                    .andExpect(jsonPath("$.data.posts[0].company").isString())
                    .andExpect(jsonPath("$.data.posts[0].url").isString())
                    .andExpect(jsonPath("$.data.posts[0].logoUrl").exists())
                    .andExpect(jsonPath("$.data.posts[0].thumbnailUrl").exists())
                    .andExpect(jsonPath("$.data.posts[0].publishedAt").exists())
                    .andExpect(jsonPath("$.data.posts[0].viewCount").isNumber())
                    .andExpect(jsonPath("$.data.posts[0].keywords").isArray())
                    .andExpect(jsonPath("$.data.posts[1].title").value("어제의 게시글"));
        }

        @Test
        @DisplayName("POPULAR 정렬로 인기 게시글 조회")
        void popularSort_ReturnsRecentPosts() throws Exception {
            setViewCount(todayPost, 100L);
            setViewCount(oldPost, 50L);
            postRepository.saveAll(List.of(todayPost, oldPost));

            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("sortBy", "POPULAR")
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
                    .andExpect(jsonPath("$.data.posts[0].title").value("오늘의 게시글"))
                    .andExpect(jsonPath("$.data.posts[0].shortSummary").value("오늘 짧은 요약"))
                    .andExpect(jsonPath("$.data.posts[0].company").isString())
                    .andExpect(jsonPath("$.data.posts[0].url").isString())
                    .andExpect(jsonPath("$.data.posts[0].logoUrl").exists())
                    .andExpect(jsonPath("$.data.posts[0].thumbnailUrl").exists())
                    .andExpect(jsonPath("$.data.posts[0].publishedAt").exists())
                    .andExpect(jsonPath("$.data.posts[0].viewCount").isNumber())
                    .andExpect(jsonPath("$.data.posts[0].keywords").isArray())
                    .andExpect(jsonPath("$.data.posts[1].title").value("어제의 게시글"));
        }

        @Test
        @DisplayName("LATEST 커서 페이징")
        void latestSortWithCursor_ReturnsNextPage() throws Exception {
            for (int i = 1; i <= 5; i++) {
                postRepository.save(PostFixture.createPost(
                        testTechBlog1,
                        "게시글 " + i,
                        "<p>내용 " + i + "</p>",
                        "내용 " + i,
                        null,
                        null,
                        "https://kakao.com/thumbnail.png",
                        "https://kakao.com/post/" + i,
                        LocalDateTime.now().minusHours(i + 1)
                ));
            }

            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("sortBy", "LATEST")
                            .param("size", "3"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts.length()").value(3))
                    .andExpect(jsonPath("$.data.hasNext").value(true));
        }

        @Test
        @DisplayName("POPULAR 커서 페이징")
        void popularSortWithCursor_ReturnsNextPage() throws Exception {
            for (int i = 1; i <= 5; i++) {
                Post post = PostFixture.createPost(
                        testTechBlog1,
                        "인기 게시글 " + i,
                        "<p>내용 " + i + "</p>",
                        "내용 " + i,
                        null,
                        null,
                        "https://kakao.com/thumbnail.png",
                        "https://kakao.com/post/popular/" + i,
                        LocalDateTime.now().minusHours(i)
                );
                setViewCount(post, (long) ((6 - i) * 100));
                postRepository.save(post);
            }

            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("sortBy", "POPULAR")
                            .param("size", "3"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts.length()").value(3))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.lastViewCount").exists());
        }

        @Test
        @DisplayName("기본값은 LATEST 정렬")
        void sortMissing_DefaultsToLatest() throws Exception {
            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts[0].title").value("오늘의 게시글"));
        }

        @Test
        @DisplayName("게시글이 없으면 빈 배열 반환")
        void noPosts_ReturnsEmptyPage() throws Exception {
            bookmarkRepository.deleteAll();
            postRepository.deleteAll();

            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("sortBy", "LATEST")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(0))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("비로그인 시 isBookmarked 미포함")
        void withoutAuth_ReturnsRecentPosts() throws Exception {
            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("sortBy", "LATEST")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts[0].isBookmarked").doesNotExist())
                    .andExpect(jsonPath("$.data.posts[1].isBookmarked").doesNotExist());
        }

        @Test
        @DisplayName("로그인 시 북마크 여부 포함 (todayPost=true, oldPost=false)")
        void withAuth_ReturnsRecentPostsWithBookmarkFlags() throws Exception {
            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("sortBy", "LATEST")
                            .param("size", "20")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2))
                    .andExpect(jsonPath("$.data.posts[0].id").value(todayPost.getId()))
                    .andExpect(jsonPath("$.data.posts[0].isBookmarked").value(true))
                    .andExpect(jsonPath("$.data.posts[1].id").value(oldPost.getId()))
                    .andExpect(jsonPath("$.data.posts[1].isBookmarked").value(false));
        }
    }

    private void setViewCount(Post post, Long viewCount) {
        ReflectionTestUtils.setField(post, "viewCount", viewCount);
    }
}
