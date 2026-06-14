package com.techfork.post.integration;

import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.auth.security.jwt.JwtUtil;
import com.techfork.post.domain.Post;
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

class PostControllerV2IntegrationTest extends IntegrationTestBase {

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
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
        testUser = userRepository.save(testUser);
        accessToken = jwtUtil.generateTokens(testUser.getId(), Role.USER).accessToken();

        testTechBlog1 = TechBlog.builder()
                .companyName("카카오")
                .blogUrl("https://kakao.com")
                .rssUrl("https://kakao.com/rss")
                .logoUrl("https://kakao.com/logo.png")
                .build();
        techBlogRepository.save(testTechBlog1);

        testTechBlog2 = TechBlog.builder()
                .companyName("네이버")
                .blogUrl("https://naver.com")
                .rssUrl("https://naver.com/rss")
                .logoUrl("https://naver.com/logo.png")
                .build();
        techBlogRepository.save(testTechBlog2);

        todayPost = Post.builder()
                .title("오늘의 게시글")
                .fullContent("<p>오늘 내용</p>")
                .plainContent("오늘 내용")
                .summary("오늘 요약")
                .shortSummary("오늘 짧은 요약")
                .company("카카오")
                .url("https://kakao.com/post/today")
                .logoUrl("https://kakao.com/logo.png")
                .thumbnailUrl("https://kakao.com/thumbnail.png")
                .publishedAt(LocalDate.now().atStartOfDay())
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog1)
                .build();
        postRepository.save(todayPost);

        oldPost = Post.builder()
                .title("어제의 게시글")
                .fullContent("<p>어제 내용</p>")
                .plainContent("어제 내용")
                .summary("어제 요약")
                .shortSummary("어제 짧은 요약")
                .company("네이버")
                .url("https://naver.com/post/old")
                .logoUrl("https://naver.com/logo.png")
                .thumbnailUrl("https://naver.com/thumbnail.png")
                .publishedAt(LocalDate.now().minusDays(1).atStartOfDay())
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog2)
                .build();
        postRepository.save(oldPost);

        bookmarkRepository.save(Bookmark.create(testUser, todayPost, LocalDateTime.now()));
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
        void getCompanies_Success() throws Exception {
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
        void getCompanies_EmptyWhenNoPosts() throws Exception {
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
        void getCompanies_SortedByLatestPublishedAt() throws Exception {
            Post newerPost = Post.builder()
                    .title("최신 네이버 게시글")
                    .fullContent("<p>최신 내용</p>")
                    .plainContent("최신 내용")
                    .company("네이버")
                    .url("https://naver.com/post/newest")
                    .logoUrl("https://naver.com/logo.png")
                    .publishedAt(LocalDateTime.now())
                    .crawledAt(LocalDateTime.now())
                    .techBlog(testTechBlog2)
                    .build();
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
        void getPostsByCompany_MultipleCompanies_Success() throws Exception {
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
        void getPostsByCompany_NoCompaniesParam_ReturnsAll() throws Exception {
            mockMvc.perform(get("/api/v2/posts/by-company")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts.length()").value(2));
        }

        @Test
        @DisplayName("단일 회사만 조회")
        void getPostsByCompany_SingleCompany_Success() throws Exception {
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
        void getPostsByCompany_CursorPaging_Success() throws Exception {
            for (int i = 1; i <= 5; i++) {
                postRepository.save(Post.builder()
                        .title("카카오 게시글 " + i)
                        .fullContent("<p>내용 " + i + "</p>")
                        .plainContent("내용 " + i)
                        .company("카카오")
                        .url("https://kakao.com/post/" + i)
                        .logoUrl("https://kakao.com/logo.png")
                        .thumbnailUrl("https://kakao.com/thumbnail.png")
                        .publishedAt(LocalDateTime.now().minusHours(i))
                        .crawledAt(LocalDateTime.now())
                        .techBlog(testTechBlog1)
                        .build());
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
        void getPostsByCompany_NonExistentCompany_ReturnsEmpty() throws Exception {
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
        void getPostsByCompany_SortedByPublishedAt() throws Exception {
            postRepository.save(Post.builder()
                    .title("최신 게시글")
                    .fullContent("<p>최신</p>")
                    .plainContent("최신")
                    .company("카카오")
                    .url("https://kakao.com/post/recent")
                    .logoUrl("https://kakao.com/logo.png")
                    .thumbnailUrl("https://kakao.com/thumbnail.png")
                    .publishedAt(LocalDateTime.now())
                    .crawledAt(LocalDateTime.now())
                    .techBlog(testTechBlog1)
                    .build());

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
        void getPostsByCompany_WithoutAuth() throws Exception {
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
        void getPostsByCompany_WithAuth() throws Exception {
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
        void getRecentPosts_Latest_Success() throws Exception {
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
        void getRecentPosts_Popular_Success() throws Exception {
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
        void getRecentPosts_Latest_CursorPaging() throws Exception {
            for (int i = 1; i <= 5; i++) {
                postRepository.save(Post.builder()
                        .title("게시글 " + i)
                        .fullContent("<p>내용 " + i + "</p>")
                        .plainContent("내용 " + i)
                        .company("카카오")
                        .url("https://kakao.com/post/" + i)
                        .logoUrl("https://kakao.com/logo.png")
                        .thumbnailUrl("https://kakao.com/thumbnail.png")
                        .publishedAt(LocalDateTime.now().minusHours(i + 1))
                        .crawledAt(LocalDateTime.now())
                        .techBlog(testTechBlog1)
                        .build());
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
        void getRecentPosts_Popular_CursorPaging() throws Exception {
            for (int i = 1; i <= 5; i++) {
                Post post = Post.builder()
                        .title("인기 게시글 " + i)
                        .fullContent("<p>내용 " + i + "</p>")
                        .plainContent("내용 " + i)
                        .company("카카오")
                        .url("https://kakao.com/post/popular/" + i)
                        .logoUrl("https://kakao.com/logo.png")
                        .thumbnailUrl("https://kakao.com/thumbnail.png")
                        .publishedAt(LocalDateTime.now().minusHours(i))
                        .crawledAt(LocalDateTime.now())
                        .techBlog(testTechBlog1)
                        .build();
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
        void getRecentPosts_DefaultSortByLatest() throws Exception {
            mockMvc.perform(get("/api/v2/posts/recent")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isArray())
                    .andExpect(jsonPath("$.data.posts[0].title").value("오늘의 게시글"));
        }

        @Test
        @DisplayName("게시글이 없으면 빈 배열 반환")
        void getRecentPosts_EmptyWhenNoPosts() throws Exception {
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
        void getRecentPosts_WithoutAuth() throws Exception {
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
        void getRecentPosts_WithAuth() throws Exception {
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
