package com.techfork.domain.activity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techfork.domain.activity.dto.BookmarkRequest;
import com.techfork.domain.activity.dto.ReadPostRequest;
import com.techfork.domain.activity.dto.SearchHistoryRequest;
import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.entity.Bookmark;
import com.techfork.domain.activity.entity.SearchHistory;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.repository.BookmarkRepository;
import com.techfork.domain.activity.repository.SearchHistoryRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.Role;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ActivityController 통합 테스트
 */
class ActivityControllerIntegrationTest extends IntegrationTestBase {

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
    private SearchHistoryRepository searchHistoryRepository;

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
        // 테스트 사용자 생성
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
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

        // 테스트 게시글 생성
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
        searchHistoryRepository.deleteAll();
        bookmarkRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ===== 읽은 게시글 저장 테스트 =====

    @Test
    @DisplayName("읽은 게시글 저장 성공 - 처음 읽는 경우 조회수 증가")
    void saveReadPost_Success_FirstRead() throws Exception {
        // Given
        Long initialViewCount = testPost1.getViewCount();
        ReadPostRequest request = new ReadPostRequest(
                testPost1.getId(),
                LocalDateTime.now(),
                300
        );

        // When & Then
        mockMvc.perform(post("/api/v1/activities/read-posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB 검증
        List<ReadPost> readPosts = readPostRepository.findAll();
        assertThat(readPosts).hasSize(1);
        assertThat(readPosts.get(0).getUser().getId()).isEqualTo(testUser.getId());
        assertThat(readPosts.get(0).getPost().getId()).isEqualTo(testPost1.getId());
        assertThat(readPosts.get(0).getReadDurationSeconds()).isEqualTo(300);

        // 조회수 증가 확인
        Post updatedPost = postRepository.findById(testPost1.getId()).orElseThrow();
        assertThat(updatedPost.getViewCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("읽은 게시글 저장 성공 - 이미 읽은 경우 조회수 증가하지 않음")
    void saveReadPost_Success_AlreadyRead() throws Exception {
        // Given - 이미 읽은 기록 생성
        ReadPost existingReadPost = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(1), 200);
        readPostRepository.save(existingReadPost);

        Long currentViewCount = testPost1.getViewCount();
        ReadPostRequest request = new ReadPostRequest(
                testPost1.getId(),
                LocalDateTime.now(),
                400
        );

        // When & Then
        mockMvc.perform(post("/api/v1/activities/read-posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB 검증 - 읽은 기록은 추가되지만 조회수는 증가하지 않음
        List<ReadPost> readPosts = readPostRepository.findAll();
        assertThat(readPosts).hasSize(2);

        Post updatedPost = postRepository.findById(testPost1.getId()).orElseThrow();
        assertThat(updatedPost.getViewCount()).isEqualTo(currentViewCount);
    }

    @Test
    @DisplayName("읽은 게시글 저장 실패 - 존재하지 않는 게시글")
    void saveReadPost_Fail_PostNotFound() throws Exception {
        // Given
        ReadPostRequest request = new ReadPostRequest(
                99999L,
                LocalDateTime.now(),
                300
        );

        // When & Then
        mockMvc.perform(post("/api/v1/activities/read-posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    // ===== 검색 히스토리 저장 테스트 =====

    @Test
    @DisplayName("검색 히스토리 저장 성공")
    void saveSearchHistory_Success() throws Exception {
        // Given
        SearchHistoryRequest request = new SearchHistoryRequest(
                "Spring Boot",
                LocalDateTime.now()
        );

        // When & Then
        mockMvc.perform(post("/api/v1/activities/searches")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB 검증
        List<SearchHistory> searchHistories = searchHistoryRepository.findAll();
        assertThat(searchHistories).hasSize(1);
        assertThat(searchHistories.get(0).getUser().getId()).isEqualTo(testUser.getId());
        assertThat(searchHistories.get(0).getSearchWord()).isEqualTo("Spring Boot");
    }

    @Test
    @DisplayName("검색 히스토리 저장 성공 - 여러 개 저장")
    void saveSearchHistory_Success_Multiple() throws Exception {
        // Given
        SearchHistoryRequest request1 = new SearchHistoryRequest("Spring Boot", LocalDateTime.now());
        SearchHistoryRequest request2 = new SearchHistoryRequest("Java", LocalDateTime.now());
        SearchHistoryRequest request3 = new SearchHistoryRequest("Kotlin", LocalDateTime.now());

        // When & Then
        mockMvc.perform(post("/api/v1/activities/searches")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/activities/searches")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/activities/searches")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        // DB 검증
        List<SearchHistory> searchHistories = searchHistoryRepository.findAll();
        assertThat(searchHistories).hasSize(3);
    }

    // ===== 북마크 추가 테스트 =====

    @Test
    @DisplayName("북마크 추가 성공")
    void addBookmark_Success() throws Exception {
        // Given
        BookmarkRequest request = new BookmarkRequest(testPost1.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB 검증
        List<Bookmark> bookmarks = bookmarkRepository.findAll();
        assertThat(bookmarks).hasSize(1);
        assertThat(bookmarks.get(0).getUser().getId()).isEqualTo(testUser.getId());
        assertThat(bookmarks.get(0).getPost().getId()).isEqualTo(testPost1.getId());
    }

    @Test
    @DisplayName("북마크 추가 실패 - 이미 북마크한 게시글")
    void addBookmark_Fail_AlreadyExists() throws Exception {
        // Given - 이미 북마크 생성
        Bookmark existingBookmark = Bookmark.create(testUser, testPost1, LocalDateTime.now());
        bookmarkRepository.save(existingBookmark);

        BookmarkRequest request = new BookmarkRequest(testPost1.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("ACTIVITY409_1"));
    }

    @Test
    @DisplayName("북마크 추가 실패 - 존재하지 않는 게시글")
    void addBookmark_Fail_PostNotFound() throws Exception {
        // Given
        BookmarkRequest request = new BookmarkRequest(99999L);

        // When & Then
        mockMvc.perform(post("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    // ===== 북마크 삭제 테스트 =====

    @Test
    @DisplayName("북마크 삭제 성공")
    void deleteBookmark_Success() throws Exception {
        // Given - 북마크 생성
        Bookmark bookmark = Bookmark.create(testUser, testPost1, LocalDateTime.now());
        bookmarkRepository.save(bookmark);

        BookmarkRequest request = new BookmarkRequest(testPost1.getId());

        // When & Then
        mockMvc.perform(delete("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // DB 검증 - 삭제되었는지 확인
        List<Bookmark> bookmarks = bookmarkRepository.findAll();
        assertThat(bookmarks).isEmpty();
    }

    @Test
    @DisplayName("북마크 삭제 실패 - 북마크가 존재하지 않음")
    void deleteBookmark_Fail_NotFound() throws Exception {
        // Given - 북마크 없음
        BookmarkRequest request = new BookmarkRequest(testPost1.getId());

        // When & Then
        mockMvc.perform(delete("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("ACTIVITY404_1"));
    }

    // ===== 북마크 목록 조회 테스트 =====

    @Test
    @DisplayName("북마크 목록 조회 성공 - 빈 목록")
    void getBookmarks_Success_Empty() throws Exception {
        // When & Then
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
    @DisplayName("북마크 목록 조회 성공 - 여러 개")
    void getBookmarks_Success_Multiple() throws Exception {
        // Given - 여러 개의 북마크 생성
        Bookmark bookmark1 = Bookmark.create(testUser, testPost1, LocalDateTime.now().minusHours(1));
        Bookmark bookmark2 = Bookmark.create(testUser, testPost2, LocalDateTime.now());
        bookmarkRepository.save(bookmark1);
        bookmarkRepository.save(bookmark2);

        // When & Then
        mockMvc.perform(get("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.bookmarks").isArray())
                .andExpect(jsonPath("$.data.bookmarks.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                // 첫 번째 북마크 DTO 전체 필드 검증 (최신순이므로 bookmark2)
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
    @DisplayName("북마크 목록 조회 성공 - 커서 기반 페이징")
    void getBookmarks_Success_WithCursor() throws Exception {
        // Given - 여러 개의 북마크 생성
        Bookmark bookmark1 = Bookmark.create(testUser, testPost1, LocalDateTime.now().minusHours(2));
        Bookmark bookmark2 = Bookmark.create(testUser, testPost2, LocalDateTime.now().minusHours(1));
        bookmarkRepository.save(bookmark1);
        bookmarkRepository.save(bookmark2);

        // When & Then - 첫 페이지 조회
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

    // ===== 읽은 게시글 조회 테스트 =====

    @Test
    @DisplayName("읽은 게시글 목록 조회 성공 - 빈 목록")
    void getReadPosts_Success_Empty() throws Exception {
        // When & Then
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
    @DisplayName("읽은 게시글 목록 조회 성공 - 여러 개")
    void getReadPosts_Success_Multiple() throws Exception {
        // Given - 읽은 게시글 기록 생성 (순서대로 저장)
        ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(2), 300);
        ReadPost readPost2 = ReadPost.create(testUser, testPost2, LocalDateTime.now().minusHours(1), 150);
        readPostRepository.save(readPost1);
        readPostRepository.save(readPost2);

        // When & Then
        mockMvc.perform(get("/api/v1/activities/read-posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.readPosts").isArray())
                .andExpect(jsonPath("$.data.readPosts.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                // 첫 번째 읽은 게시글 DTO 전체 필드 검증 (ID 역순이므로 readPost2)
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
    @DisplayName("읽은 게시글 목록 조회 성공 - 동일 포스트 중복 제거 확인")
    void getReadPosts_Success_Deduplicated() throws Exception {
        // Given - 동일한 포스트(testPost1)를 두 번 읽음
        ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(2), 300);
        ReadPost readPost2 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(1), 400);
        // 다른 포스트(testPost2)를 읽음
        ReadPost readPost3 = ReadPost.create(testUser, testPost2, LocalDateTime.now(), 150);

        readPostRepository.saveAll(List.of(readPost1, readPost2, readPost3));

        // When & Then
        mockMvc.perform(get("/api/v1/activities/read-posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readPosts.length()").value(2)) // 3개가 아닌 2개여야 함
                .andExpect(jsonPath("$.data.readPosts[0].postId").value(testPost2.getId()))
                .andExpect(jsonPath("$.data.readPosts[1].postId").value(testPost1.getId()))
                .andExpect(jsonPath("$.data.readPosts[1].readPostId").value(readPost2.getId())); // 더 최신 ID
    }

    @Test
    @DisplayName("읽은 게시글 목록 조회 성공 - 북마크 상태 포함")
    void getReadPosts_Success_WithBookmarks() throws Exception {
        // Given - 읽은 게시글 기록 생성 (순서대로 저장)
        ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(2), 300);
        ReadPost readPost2 = ReadPost.create(testUser, testPost2, LocalDateTime.now().minusHours(1), 150);
        readPostRepository.save(readPost1);
        readPostRepository.save(readPost2);

        // Given - testPost2만 북마크 (readPost2가 먼저 조회됨)
        Bookmark bookmark = Bookmark.create(testUser, testPost2, LocalDateTime.now());
        bookmarkRepository.save(bookmark);

        // When & Then
        mockMvc.perform(get("/api/v1/activities/read-posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readPosts.length()").value(2))
                .andExpect(jsonPath("$.data.readPosts[0].postId").value(testPost2.getId()))
                .andExpect(jsonPath("$.data.readPosts[0].isBookmarked").value(true))  // testPost2
                .andExpect(jsonPath("$.data.readPosts[1].postId").value(testPost1.getId()))
                .andExpect(jsonPath("$.data.readPosts[1].isBookmarked").value(false)); // testPost1
    }

    @Test
    @DisplayName("읽은 게시글 목록 조회 성공 - 커서 기반 페이징")
    void getReadPosts_Success_WithCursor() throws Exception {
        // Given - 여러 개의 읽은 게시글 생성
        ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(1), 300);
        ReadPost readPost2 = ReadPost.create(testUser, testPost2, LocalDateTime.now().minusHours(2), 150);
        readPostRepository.save(readPost1);
        readPostRepository.save(readPost2);

        // When & Then - 첫 페이지 조회
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

    // ===== 통합 시나리오 테스트 =====

    @Test
    @DisplayName("통합 시나리오 - 북마크 추가 후 조회 후 삭제")
    void integrationScenario_AddGetDeleteBookmark() throws Exception {
        // 1. 북마크 추가
        BookmarkRequest addRequest = new BookmarkRequest(testPost1.getId());
        mockMvc.perform(post("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated());

        // 2. 북마크 목록 조회
        mockMvc.perform(get("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarks.length()").value(1))
                .andExpect(jsonPath("$.data.bookmarks[0].postId").value(testPost1.getId()));

        // 3. 북마크 삭제
        BookmarkRequest deleteRequest = new BookmarkRequest(testPost1.getId());
        mockMvc.perform(delete("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isOk());

        // 4. 삭제 후 목록 조회 - 빈 목록
        mockMvc.perform(get("/api/v1/activities/bookmarks")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarks").isEmpty());
    }

    @Test
    @DisplayName("통합 시나리오 - 게시글 읽기 후 읽은 목록 조회")
    void integrationScenario_ReadPost_GetReadPosts() throws Exception {
        // 1. 게시글 읽기 기록 저장
        ReadPostRequest readRequest = new ReadPostRequest(
                testPost1.getId(),
                LocalDateTime.now(),
                300
        );
        mockMvc.perform(post("/api/v1/activities/read-posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(readRequest)))
                .andExpect(status().isCreated());

        // 2. 읽은 게시글 목록 조회
        mockMvc.perform(get("/api/v1/activities/read-posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readPosts.length()").value(1))
                .andExpect(jsonPath("$.data.readPosts[0].postId").value(testPost1.getId()));
    }
}
