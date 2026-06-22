package com.techfork.activity.bookmark.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.bookmark.fixture.BookmarkFixture;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.activity.bookmark.presentation.BookmarkRequest;
import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.fixture.TechBlogFixture;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.fixture.UserFixture;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.auth.security.jwt.JwtDTO;
import com.techfork.auth.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookmarkIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String accessToken;
    private Post testPost1;
    private Post testPost2;
    private TechBlog testBlog;

    @BeforeEach
    void setUp() {
        testUser = UserFixture.activeUser("testSocialId", "test@example.com");
        testUser = userRepository.save(testUser);

        JwtDTO tokens = jwtUtil.generateTokens(testUser.getId(), Role.USER);
        accessToken = tokens.accessToken();

        testBlog = TechBlogFixture.createTechBlog("테스트회사", "https://test.com");
        testBlog = techBlogRepository.save(testBlog);

        testPost1 = PostFixture.createPost(testBlog, "테스트 게시글 1",
                "게시글 1의 전체 내용입니다.", "게시글 1의 내용",
                "게시글 1의 요약", "게시글 1의 짧은 요약", "https://test.com/thumb1.png",
                "https://test.com/post/1", LocalDateTime.now().minusDays(1));
        testPost1 = postRepository.save(testPost1);

        testPost2 = PostFixture.createPost(testBlog, "테스트 게시글 2",
                "게시글 2의 전체 내용입니다.", "게시글 2의 내용",
                "게시글 2의 요약", "게시글 2의 짧은 요약", "https://test.com/thumb2.png",
                "https://test.com/post/2", LocalDateTime.now().minusDays(2));
        testPost2 = postRepository.save(testPost2);
    }

    @AfterEach
    void tearDown() {
        bookmarkRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("북마크 추가")
    class AddBookmark {

        @Test
        @DisplayName("북마크 추가 성공")
        void validRequest_ReturnsOk() throws Exception {
            BookmarkRequest request = new BookmarkRequest(testPost1.getId());

            mockMvc.perform(post("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            List<Bookmark> bookmarks = bookmarkRepository.findAll();
            assertThat(bookmarks).hasSize(1);
            assertThat(bookmarks.get(0).getUser().getId()).isEqualTo(testUser.getId());
            assertThat(bookmarks.get(0).getPost().getId()).isEqualTo(testPost1.getId());
        }

        @Test
        @DisplayName("이미 북마크한 게시글")
        void alreadyBookmarkedPost_ReturnsConflict() throws Exception {
            Bookmark existingBookmark = BookmarkFixture.createBookmark(testUser, testPost1, LocalDateTime.now());
            bookmarkRepository.save(existingBookmark);

            BookmarkRequest request = new BookmarkRequest(testPost1.getId());

            mockMvc.perform(post("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("BOOKMARK409_1"));
        }

        @Test
        @DisplayName("존재하지 않는 게시글")
        void postNotFound_ReturnsNotFound() throws Exception {
            BookmarkRequest request = new BookmarkRequest(99999L);

            mockMvc.perform(post("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("북마크 삭제")
    class DeleteBookmark {

        @Test
        @DisplayName("북마크 삭제 성공")
        void existingBookmark_ReturnsOk() throws Exception {
            Bookmark bookmark = BookmarkFixture.createBookmark(testUser, testPost1, LocalDateTime.now());
            bookmarkRepository.save(bookmark);

            BookmarkRequest request = new BookmarkRequest(testPost1.getId());

            mockMvc.perform(delete("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            List<Bookmark> bookmarks = bookmarkRepository.findAll();
            assertThat(bookmarks).isEmpty();
        }

        @Test
        @DisplayName("북마크가 존재하지 않음")
        void bookmarkNotFound_ReturnsNotFound() throws Exception {
            BookmarkRequest request = new BookmarkRequest(testPost1.getId());

            mockMvc.perform(delete("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("BOOKMARK404_1"));
        }
    }

    @Nested
    @DisplayName("북마크 목록 조회")
    class GetBookmarks {

        @Test
        @DisplayName("빈 목록")
        void noBookmarks_ReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.bookmarks").isArray())
                    .andExpect(jsonPath("$.data.bookmarks").isEmpty())
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("여러 개")
        void multipleBookmarks_ReturnsBookmarks() throws Exception {
            Bookmark bookmark1 = BookmarkFixture.createBookmark(testUser, testPost1, LocalDateTime.now().minusHours(1));
            Bookmark bookmark2 = BookmarkFixture.createBookmark(testUser, testPost2, LocalDateTime.now());
            bookmarkRepository.save(bookmark1);
            bookmarkRepository.save(bookmark2);

            mockMvc.perform(get("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.bookmarks").isArray())
                    .andExpect(jsonPath("$.data.bookmarks.length()").value(2))
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.bookmarks[0].bookmarkId").value(bookmark2.getId()))
                    .andExpect(jsonPath("$.data.bookmarks[0].postId").value(testPost2.getId()))
                    .andExpect(jsonPath("$.data.bookmarks[0].title").value("테스트 게시글 2"))
                    .andExpect(jsonPath("$.data.bookmarks[0].shortSummary").value("게시글 2의 짧은 요약"))
                    .andExpect(jsonPath("$.data.bookmarks[0].url").value("https://test.com/post/2"))
                    .andExpect(jsonPath("$.data.bookmarks[0].companyName").value("테스트회사"))
                    .andExpect(jsonPath("$.data.bookmarks[0].logoUrl").value("https://test.com/logo.png"))
                    .andExpect(jsonPath("$.data.bookmarks[0].publishedAt").exists())
                    .andExpect(jsonPath("$.data.bookmarks[0].thumbnailUrl").value("https://test.com/thumb2.png"))
                    .andExpect(jsonPath("$.data.bookmarks[0].viewCount").value(0))
                    .andExpect(jsonPath("$.data.bookmarks[0].keywords").isArray())
                    .andExpect(jsonPath("$.data.bookmarks[0].isBookmarked").value(true));
        }

        @Test
        @DisplayName("커서 기반 페이징")
        void cursorProvided_ReturnsNextPage() throws Exception {
            Bookmark bookmark1 = BookmarkFixture.createBookmark(testUser, testPost1, LocalDateTime.now().minusHours(2));
            Bookmark bookmark2 = BookmarkFixture.createBookmark(testUser, testPost2, LocalDateTime.now().minusHours(1));
            bookmarkRepository.save(bookmark1);
            bookmarkRepository.save(bookmark2);

            mockMvc.perform(get("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("size", "1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.bookmarks.length()").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.lastBookmarkId").exists());
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class Scenario {

        @Test
        @DisplayName("북마크 추가 후 조회 후 삭제")
        void addGetDeleteFlow_ReflectsBookmarkLifecycle() throws Exception {
            BookmarkRequest addRequest = new BookmarkRequest(testPost1.getId());
            mockMvc.perform(post("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addRequest)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.bookmarks.length()").value(1))
                    .andExpect(jsonPath("$.data.bookmarks[0].postId").value(testPost1.getId()));

            BookmarkRequest deleteRequest = new BookmarkRequest(testPost1.getId());
            mockMvc.perform(delete("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(deleteRequest)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/activities/bookmarks")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.bookmarks").isEmpty());
        }
    }
}
