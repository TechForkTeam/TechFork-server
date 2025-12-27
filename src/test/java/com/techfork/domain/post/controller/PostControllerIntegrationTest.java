package com.techfork.domain.post.controller;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.entity.PostKeyword;
import com.techfork.domain.post.repository.PostKeywordRepository;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostController 통합 테스트
 * - @SpringBootTest: 전체 애플리케이션 컨텍스트 로드
 * - Testcontainers: 실제 MySQL 컨테이너로 통합 테스트
 * - 모든 레이어(Controller, Service, Repository) 통합 테스트
 * - MockMvc로 HTTP 요청/응답 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("integrationtest")
class PostControllerIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostKeywordRepository postKeywordRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    private TechBlog testTechBlog;
    private Post testPost1;
    private Post testPost2;

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
                .company("테스트 회사")
                .url("https://test.com/post/1")
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
                .company("테스트 회사")
                .url("https://test.com/post/2")
                .publishedAt(LocalDateTime.of(2025, 1, 2, 10, 0))
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog)
                .build();
        testPost2 = postRepository.save(testPost2);

        PostKeyword keyword3 = PostKeyword.create("Kotlin", testPost2);
        postKeywordRepository.save(keyword3);
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리 (외래키 제약조건 순서 고려)
        postKeywordRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 게시글 상세 조회 성공")
    void getPostDetail_Success() throws Exception {
        // When & Then: 실제 DB에 저장된 데이터로 HTTP 요청 테스트
        mockMvc.perform(get("/api/v1/posts/{postId}", testPost1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testPost1.getId()))
                .andExpect(jsonPath("$.data.title").value("테스트 게시글 1"))
                .andExpect(jsonPath("$.data.company").value("테스트 회사"))
                .andExpect(jsonPath("$.data.keywords").isArray())
                .andExpect(jsonPath("$.data.keywords.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/posts/recent - 최근 게시글 조회 성공")
    void getRecentPosts_Success() throws Exception {
        // When & Then: 실제 DB에서 최근 게시글 조회
        mockMvc.perform(get("/api/v1/posts/recent")
                        .param("sortBy", "LATEST")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/posts/companies - 회사 목록 조회 성공")
    void getCompanies_Success() throws Exception {
        // When & Then: 실제 DB에서 회사 목록 조회
        mockMvc.perform(get("/api/v1/posts/companies"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companies").isArray())
                .andExpect(jsonPath("$.data.companies[0]").value("테스트 회사"))
                .andExpect(jsonPath("$.data.companies.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 존재하지 않는 게시글 조회 시 404")
    void getPostDetail_NotFound() throws Exception {
        // When & Then: 존재하지 않는 게시글 조회
        mockMvc.perform(get("/api/v1/posts/{postId}", 99999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
