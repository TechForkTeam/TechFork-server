package com.techfork.domain.post.controller;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.global.common.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostControllerV2 통합 테스트
 * - 모든 레이어(Controller, Service, Repository) 통합 테스트
 * - MockMvc로 HTTP 요청/응답 테스트
 */
class PostControllerV2IntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    private TechBlog testTechBlog1;
    private TechBlog testTechBlog2;
    private Post todayPost;
    private Post oldPost;

    @BeforeEach
    void setUp() {
        // Given: 실제 DB에 테스트 데이터 저장
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

        // 오늘 발행된 게시글 (카카오)
        todayPost = Post.builder()
                .title("오늘의 게시글")
                .fullContent("<p>오늘 내용</p>")
                .plainContent("오늘 내용")
                .company("카카오")
                .url("https://kakao.com/post/today")
                .logoUrl("https://kakao.com/logo.png")
                .thumbnailUrl("https://kakao.com/thumbnail.png")
                .publishedAt(LocalDate.now().atStartOfDay())
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog1)
                .build();
        postRepository.save(todayPost);

        // 어제 발행된 게시글 (네이버)
        oldPost = Post.builder()
                .title("어제의 게시글")
                .fullContent("<p>어제 내용</p>")
                .plainContent("어제 내용")
                .company("네이버")
                .url("https://naver.com/post/old")
                .logoUrl("https://naver.com/logo.png")
                .thumbnailUrl("https://naver.com/thumbnail.png")
                .publishedAt(LocalDate.now().minusDays(1).atStartOfDay())
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog2)
                .build();
        postRepository.save(oldPost);
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리 (외래키 제약조건 순서 고려)
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v2/posts/companies - 회사 목록 상세 조회 성공")
    void getCompanies_Success() throws Exception {
        // When & Then: 실제 DB에서 회사 상세 정보 조회
        mockMvc.perform(get("/api/v2/posts/companies"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNumber").value(2))
                .andExpect(jsonPath("$.data.companies").isArray())
                .andExpect(jsonPath("$.data.companies.length()").value(2))
                // 첫 번째 회사 (카카오 - 오늘 발행)
                .andExpect(jsonPath("$.data.companies[0].company").value("카카오"))
                .andExpect(jsonPath("$.data.companies[0].hasNewPost").value(true))
                .andExpect(jsonPath("$.data.companies[0].logoUrl").value("https://kakao.com/logo.png"))
                // 두 번째 회사 (네이버 - 어제 발행)
                .andExpect(jsonPath("$.data.companies[1].company").value("네이버"))
                .andExpect(jsonPath("$.data.companies[1].hasNewPost").value(false))
                .andExpect(jsonPath("$.data.companies[1].logoUrl").value("https://naver.com/logo.png"));
    }

    @Test
    @DisplayName("GET /api/v2/posts/companies - 게시글이 없는 경우 빈 배열 반환")
    void getCompanies_EmptyWhenNoPosts() throws Exception {
        // Given: 모든 게시글 삭제
        postRepository.deleteAll();

        // When & Then: 빈 배열 반환 확인
        mockMvc.perform(get("/api/v2/posts/companies"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNumber").value(0))
                .andExpect(jsonPath("$.data.companies").isArray())
                .andExpect(jsonPath("$.data.companies.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v2/posts/companies - 발행일 기준 정렬 확인")
    void getCompanies_SortedByLatestPublishedAt() throws Exception {
        // Given: 새로운 게시글 추가 (네이버가 최근)
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

        // When & Then: 네이버가 첫 번째로 정렬되는지 확인
        mockMvc.perform(get("/api/v2/posts/companies"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companies[0].company").value("네이버"))
                .andExpect(jsonPath("$.data.companies[1].company").value("카카오"));
    }

    @Test
    @DisplayName("GET /api/v2/posts/by-company - 여러 회사의 게시글 조회 성공")
    void getPostsByCompany_MultipleCompanies_Success() throws Exception {
        mockMvc.perform(get("/api/v2/posts/by-company")
                        .param("companies", "카카오", "네이버")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                .andExpect(jsonPath("$.data.posts[0].company").value("카카오"))
                .andExpect(jsonPath("$.data.posts[1].company").value("네이버"))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.lastPostId").exists())
                .andExpect(jsonPath("$.data.lastPublishedAt").exists());
    }

    @Test
    @DisplayName("GET /api/v2/posts/by-company - companies 없으면 전체 조회")
    void getPostsByCompany_NoCompaniesParam_ReturnsAll() throws Exception {
        mockMvc.perform(get("/api/v2/posts/by-company")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v2/posts/by-company - 단일 회사만 조회")
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
    @DisplayName("GET /api/v2/posts/by-company - 커서 페이징")
    void getPostsByCompany_CursorPaging_Success() throws Exception {
        // Given: 추가 게시글 생성
        for (int i = 1; i <= 5; i++) {
            Post post = Post.builder()
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
                    .build();
            postRepository.save(post);
        }

        // When: 첫 페이지 조회 (size=3)
        mockMvc.perform(get("/api/v2/posts/by-company")
                        .param("companies", "카카오")
                        .param("size", "3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts.length()").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("GET /api/v2/posts/by-company - 존재하지 않는 회사 조회 시 빈 배열")
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
    @DisplayName("GET /api/v2/posts/by-company - 발행일 기준 정렬 확인")
    void getPostsByCompany_SortedByPublishedAt() throws Exception {
        // Given: 여러 시간대의 게시글 추가
        Post recentPost = Post.builder()
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
                .build();
        postRepository.save(recentPost);

        // When & Then: 최신 게시글이 먼저 오는지 확인
        mockMvc.perform(get("/api/v2/posts/by-company")
                        .param("companies", "카카오")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].title").value("최신 게시글"))
                .andExpect(jsonPath("$.data.posts[1].title").value("오늘의 게시글"));
    }

    @Test
    @DisplayName("GET /api/v2/posts/recent - LATEST 정렬로 최근 게시글 조회")
    void getRecentPosts_Latest_Success() throws Exception {
        mockMvc.perform(get("/api/v2/posts/recent")
                        .param("sortBy", "LATEST")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                .andExpect(jsonPath("$.data.posts[0].title").value("오늘의 게시글"))
                .andExpect(jsonPath("$.data.posts[1].title").value("어제의 게시글"))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.lastPostId").exists())
                .andExpect(jsonPath("$.data.lastPublishedAt").exists());
    }

    @Test
    @DisplayName("GET /api/v2/posts/recent - POPULAR 정렬로 인기 게시글 조회")
    void getRecentPosts_Popular_Success() throws Exception {
        // Given: 조회수 증가
        for (int i = 0; i < 100; i++) {
            todayPost.incrementViewCount();
        }
        for (int i = 0; i < 50; i++) {
            oldPost.incrementViewCount();
        }
        postRepository.saveAll(List.of(todayPost, oldPost));

        // When & Then: 조회수 높은 순으로 정렬
        mockMvc.perform(get("/api/v2/posts/recent")
                        .param("sortBy", "POPULAR")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                .andExpect(jsonPath("$.data.posts[0].title").value("오늘의 게시글"))
                .andExpect(jsonPath("$.data.posts[1].title").value("어제의 게시글"))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.lastPostId").exists())
                .andExpect(jsonPath("$.data.lastViewCount").exists());
    }

    @Test
    @DisplayName("GET /api/v2/posts/recent - LATEST 커서 페이징")
    void getRecentPosts_Latest_CursorPaging() throws Exception {
        // Given: 추가 게시글 생성
        for (int i = 1; i <= 5; i++) {
            Post post = Post.builder()
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
                    .build();
            postRepository.save(post);
        }

        // When: 첫 페이지 조회 (size=3)
        mockMvc.perform(get("/api/v2/posts/recent")
                        .param("sortBy", "LATEST")
                        .param("size", "3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts.length()").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("GET /api/v2/posts/recent - POPULAR 커서 페이징")
    void getRecentPosts_Popular_CursorPaging() throws Exception {
        // Given: 다양한 조회수를 가진 게시글 생성
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

            // 조회수 설정
            for (int j = 0; j < (6 - i) * 100; j++) {
                post.incrementViewCount();
            }
            postRepository.save(post);
        }

        // When: 첫 페이지 조회 (size=3)
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
    @DisplayName("GET /api/v2/posts/recent - 기본값은 LATEST 정렬")
    void getRecentPosts_DefaultSortByLatest() throws Exception {
        mockMvc.perform(get("/api/v2/posts/recent")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts[0].title").value("오늘의 게시글"));
    }

    @Test
    @DisplayName("GET /api/v2/posts/recent - 게시글이 없으면 빈 배열 반환")
    void getRecentPosts_EmptyWhenNoPosts() throws Exception {
        // Given: 모든 게시글 삭제
        postRepository.deleteAll();

        // When & Then: 빈 배열 반환
        mockMvc.perform(get("/api/v2/posts/recent")
                        .param("sortBy", "LATEST")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }
}