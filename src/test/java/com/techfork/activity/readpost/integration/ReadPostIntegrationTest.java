package com.techfork.activity.readpost.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.activity.bookmark.entity.Bookmark;
import com.techfork.activity.bookmark.repository.BookmarkRepository;
import com.techfork.activity.readpost.dto.ReadPostRequest;
import com.techfork.activity.readpost.entity.ReadPost;
import com.techfork.activity.readpost.repository.ReadPostRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.enums.Role;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.common.MySqlRedisIntegrationTestBase;
import com.techfork.global.security.jwt.JwtDTO;
import com.techfork.global.security.jwt.JwtUtil;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReadPostIntegrationTest extends MySqlRedisIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private ReadPostRepository readPostRepository;

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
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
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

        testPost1 = Post.builder()
                .title("테스트 게시글 1")
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
                .title("테스트 게시글 2")
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
    }

    @AfterEach
    void tearDown() {
        readPostRepository.deleteAll();
        bookmarkRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("읽은 게시글 저장")
    class SaveReadPost {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("처음 읽는 경우 조회수가 증가한다")
            void saveReadPost_Success_FirstRead() throws Exception {
                Long initialViewCount = testPost1.getViewCount();
                ReadPostRequest request = new ReadPostRequest(testPost1.getId(), LocalDateTime.now(), 300);

                mockMvc.perform(post("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.isSuccess").value(true));

                List<ReadPost> readPosts = readPostRepository.findAll();
                assertThat(readPosts).hasSize(1);
                assertThat(readPosts.get(0).getUser().getId()).isEqualTo(testUser.getId());
                assertThat(readPosts.get(0).getPost().getId()).isEqualTo(testPost1.getId());
                assertThat(readPosts.get(0).getReadDurationSeconds()).isEqualTo(300);

                Post updatedPost = postRepository.findById(testPost1.getId()).orElseThrow();
                assertThat(updatedPost.getViewCount()).isEqualTo(initialViewCount + 1);
            }

            @Test
            @DisplayName("이미 읽은 경우 조회수는 증가하지 않는다")
            void saveReadPost_Success_AlreadyRead() throws Exception {
                ReadPost existingReadPost = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(1), 200);
                readPostRepository.save(existingReadPost);

                Long currentViewCount = testPost1.getViewCount();
                ReadPostRequest request = new ReadPostRequest(testPost1.getId(), LocalDateTime.now(), 400);

                mockMvc.perform(post("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.isSuccess").value(true));

                List<ReadPost> readPosts = readPostRepository.findAll();
                assertThat(readPosts).hasSize(2);

                Post updatedPost = postRepository.findById(testPost1.getId()).orElseThrow();
                assertThat(updatedPost.getViewCount()).isEqualTo(currentViewCount);
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 게시글이면 실패한다")
            void saveReadPost_Fail_PostNotFound() throws Exception {
                ReadPostRequest request = new ReadPostRequest(99999L, LocalDateTime.now(), 300);

                mockMvc.perform(post("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false));
            }
        }
    }

    @Nested
    @DisplayName("읽은 게시글 목록 조회")
    class GetReadPosts {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("빈 목록")
            void getReadPosts_Success_Empty() throws Exception {
                mockMvc.perform(get("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("size", "20"))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.data.readPosts").isArray())
                        .andExpect(jsonPath("$.data.readPosts").isEmpty())
                        .andExpect(jsonPath("$.data.hasNext").value(false));
            }

            @Test
            @DisplayName("여러 개")
            void getReadPosts_Success_Multiple() throws Exception {
                ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(2), 300);
                ReadPost readPost2 = ReadPost.create(testUser, testPost2, LocalDateTime.now().minusHours(1), 150);
                readPostRepository.save(readPost1);
                readPostRepository.save(readPost2);

                mockMvc.perform(get("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("size", "20"))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.data.readPosts").isArray())
                        .andExpect(jsonPath("$.data.readPosts.length()").value(2))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.readPosts[0].readPostId").value(readPost2.getId()))
                        .andExpect(jsonPath("$.data.readPosts[0].postId").value(testPost2.getId()))
                        .andExpect(jsonPath("$.data.readPosts[0].title").value("테스트 게시글 2"))
                        .andExpect(jsonPath("$.data.readPosts[0].shortSummary").value("게시글 2의 짧은 요약"))
                        .andExpect(jsonPath("$.data.readPosts[0].url").value("https://test.com/post/2"))
                        .andExpect(jsonPath("$.data.readPosts[0].companyName").value("테스트회사"))
                        .andExpect(jsonPath("$.data.readPosts[0].logoUrl").value("https://test.com/logo.png"))
                        .andExpect(jsonPath("$.data.readPosts[0].publishedAt").exists())
                        .andExpect(jsonPath("$.data.readPosts[0].thumbnailUrl").value("https://test.com/thumb2.png"))
                        .andExpect(jsonPath("$.data.readPosts[0].viewCount").value(0))
                        .andExpect(jsonPath("$.data.readPosts[0].keywords").isArray())
                        .andExpect(jsonPath("$.data.readPosts[0].isBookmarked").value(false))
                        .andExpect(jsonPath("$.data.readPosts[0].readAt").exists());
            }

            @Test
            @DisplayName("동일 포스트는 중복 제거된다")
            void getReadPosts_Success_Deduplicated() throws Exception {
                ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(2), 300);
                ReadPost readPost2 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(1), 400);
                ReadPost readPost3 = ReadPost.create(testUser, testPost2, LocalDateTime.now(), 150);
                readPostRepository.saveAll(List.of(readPost1, readPost2, readPost3));

                mockMvc.perform(get("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("size", "20"))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.readPosts.length()").value(2))
                        .andExpect(jsonPath("$.data.readPosts[0].postId").value(testPost2.getId()))
                        .andExpect(jsonPath("$.data.readPosts[1].postId").value(testPost1.getId()))
                        .andExpect(jsonPath("$.data.readPosts[1].readPostId").value(readPost2.getId()));
            }

            @Test
            @DisplayName("북마크 상태를 포함한다")
            void getReadPosts_Success_WithBookmarks() throws Exception {
                ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(2), 300);
                ReadPost readPost2 = ReadPost.create(testUser, testPost2, LocalDateTime.now().minusHours(1), 150);
                readPostRepository.save(readPost1);
                readPostRepository.save(readPost2);

                Bookmark bookmark = Bookmark.create(testUser, testPost2, LocalDateTime.now());
                bookmarkRepository.save(bookmark);

                mockMvc.perform(get("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("size", "20"))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.readPosts.length()").value(2))
                        .andExpect(jsonPath("$.data.readPosts[0].postId").value(testPost2.getId()))
                        .andExpect(jsonPath("$.data.readPosts[0].isBookmarked").value(true))
                        .andExpect(jsonPath("$.data.readPosts[1].postId").value(testPost1.getId()))
                        .andExpect(jsonPath("$.data.readPosts[1].isBookmarked").value(false));
            }

            @Test
            @DisplayName("커서 기반 페이징")
            void getReadPosts_Success_WithCursor() throws Exception {
                ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(1), 300);
                ReadPost readPost2 = ReadPost.create(testUser, testPost2, LocalDateTime.now().minusHours(2), 150);
                readPostRepository.save(readPost1);
                readPostRepository.save(readPost2);

                mockMvc.perform(get("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("size", "1"))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.data.readPosts.length()").value(1))
                        .andExpect(jsonPath("$.data.hasNext").value(true))
                        .andExpect(jsonPath("$.data.lastReadPostId").exists());
            }
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationScenario {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("게시글 읽기 후 읽은 목록 조회")
            void integrationScenario_ReadPost_GetReadPosts() throws Exception {
                ReadPostRequest readRequest = new ReadPostRequest(testPost1.getId(), LocalDateTime.now(), 300);
                mockMvc.perform(post("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(readRequest)))
                        .andExpect(status().isCreated());

                mockMvc.perform(get("/api/v1/activities/read-posts")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("size", "20"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.readPosts.length()").value(1))
                        .andExpect(jsonPath("$.data.readPosts[0].postId").value(testPost1.getId()));
            }
        }
    }
}
