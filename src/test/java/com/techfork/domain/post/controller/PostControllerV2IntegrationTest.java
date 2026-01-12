package com.techfork.domain.post.controller;

import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.global.configuration.MySQLTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostControllerV2 통합 테스트
 * - @SpringBootTest: 전체 애플리케이션 컨텍스트 로드
 * - MySQLTestConfig.class: 실제 MySQL 컨테이너로 통합 테스트
 * - 모든 레이어(Controller, Service, Repository) 통합 테스트
 * - MockMvc로 HTTP 요청/응답 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySQLTestConfig.class)
@ActiveProfiles("integrationtest")
class PostControllerV2IntegrationTest {

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
}