package com.techfork.domain.post.repository;

import com.techfork.domain.post.dto.CompanyDto;
import com.techfork.domain.post.dto.PostDetailDto;
import com.techfork.domain.post.dto.PostInfoDto;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostRepository 테스트
 * - @DataJpaTest: JPA 관련 컴포넌트만 로드
 * - @ActiveProfiles("test"): H2 in-memory database 사용
 * - 복잡한 JPQL 쿼리, 커서 페이징, 프로젝션 검증
 */
@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    private TechBlog techBlog1;
    private TechBlog techBlog2;
    private TechBlog techBlog3;

    @BeforeEach
    void setUp() {
        // Given: 테스트용 TechBlog 생성
        techBlog1 = TechBlog.builder()
                .companyName("카카오")
                .blogUrl("https://kakao.com/blog")
                .rssUrl("https://kakao.com/rss")
                .build();
        techBlogRepository.save(techBlog1);

        techBlog2 = TechBlog.builder()
                .companyName("네이버")
                .blogUrl("https://naver.com/blog")
                .rssUrl("https://naver.com/rss")
                .build();
        techBlogRepository.save(techBlog2);

        techBlog3 = TechBlog.builder()
                .companyName("AWS")
                .blogUrl("https://aws.com/blog")
                .rssUrl("https://aws.com/rss")
                .build();
        techBlogRepository.save(techBlog3);
    }

    @Test
    @DisplayName("findPopularPostsWithCursor - lastPostId가 null이면 첫 페이지 조회 (조회수 높은 순)")
    void findPopularPostsWithCursor_FirstPage_OrderByViewCountDesc() {
        // Given: 조회수가 다른 게시글 3개
        Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(3), 100L);
        Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(2), 500L);
        Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(1), 300L);
        postRepository.saveAll(List.of(post1, post2, post3));

        // When: lastPostId = null, size = 10
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<PostInfoDto> result = postRepository.findPopularPostsWithCursor(null, pageRequest);

        // Then: 조회수 높은 순으로 정렬 (500 > 300 > 100)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).viewCount()).isEqualTo(500L);
        assertThat(result.get(1).viewCount()).isEqualTo(300L);
        assertThat(result.get(2).viewCount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("findPopularPostsWithCursor - lastPostId 지정 시 해당 ID 이후 게시글만 조회")
    void findPopularPostsWithCursor_NextPage_FilterByLastPostId() {
        // Given: 게시글 5개 생성
        Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(5), 500L);
        Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(4), 400L);
        Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(3), 300L);
        Post post4 = createPost("게시글4", techBlog1, LocalDateTime.now().minusDays(2), 200L);
        Post post5 = createPost("게시글5", techBlog1, LocalDateTime.now().minusDays(1), 100L);
        postRepository.saveAll(List.of(post1, post2, post3, post4, post5));

        // When: lastPostId = post3.id (커서 기준)
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<PostInfoDto> result = postRepository.findPopularPostsWithCursor(post3.getId(), pageRequest);

        // Then: post3.id보다 작은 ID만 반환 (post4, post5는 제외)
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> dto.id() < post3.getId());
    }

    @Test
    @DisplayName("findRecentPostsWithCursor - 최신 발행일 순으로 정렬")
    void findRecentPostsWithCursor_OrderByPublishedAtDesc() {
        // Given: 발행일이 다른 게시글 3개
        LocalDateTime now = LocalDateTime.now();
        Post post1 = createPost("게시글1", techBlog1, now.minusDays(3), 100L);
        Post post2 = createPost("게시글2", techBlog1, now.minusDays(1), 200L);
        Post post3 = createPost("게시글3", techBlog1, now.minusDays(2), 300L);
        postRepository.saveAll(List.of(post1, post2, post3));

        // When: 최신순 조회
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<PostInfoDto> result = postRepository.findRecentPostsWithCursor(null, pageRequest);

        // Then: 발행일 최신순 (post2 > post3 > post1)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).publishedAt()).isAfter(result.get(1).publishedAt());
        assertThat(result.get(1).publishedAt()).isAfter(result.get(2).publishedAt());
    }

    @Test
    @DisplayName("findByCompanyWithCursor - company가 null이면 모든 게시글 조회")
    void findByCompanyWithCursor_NullCompany_ReturnsAll() {
        // Given: 다른 회사의 게시글 2개
        Post kakaoPost = createPost("카카오 게시글", techBlog1, LocalDateTime.now(), 100L);
        Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 200L);
        postRepository.saveAll(List.of(kakaoPost, naverPost));

        // When: company = null
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<PostInfoDto> result = postRepository.findByCompanyWithCursor(null, null, pageRequest);

        // Then: 모든 게시글 반환
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PostInfoDto::company)
                .containsExactlyInAnyOrder("카카오", "네이버");
    }

    @Test
    @DisplayName("findByCompanyWithCursor - company 지정 시 해당 회사 게시글만 조회")
    void findByCompanyWithCursor_SpecificCompany_ReturnsFiltered() {
        // Given: 다른 회사의 게시글 3개
        Post kakaoPost1 = createPost("카카오 게시글1", techBlog1, LocalDateTime.now().minusDays(2), 100L);
        Post kakaoPost2 = createPost("카카오 게시글2", techBlog1, LocalDateTime.now().minusDays(1), 200L);
        Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 300L);
        postRepository.saveAll(List.of(kakaoPost1, kakaoPost2, naverPost));

        // When: company = "카카오"
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<PostInfoDto> result = postRepository.findByCompanyWithCursor("카카오", null, pageRequest);

        // Then: 카카오 게시글만 반환
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> dto.company().equals("카카오"));
    }

    @Test
    @DisplayName("companies가 null이면 모든 회사의 게시글을 조회")
    void findByCompanyNames_NullCompanies_ReturnsAll() {
        // Given
        Post kakaoPost = createPost("카카오 게시글", techBlog1, LocalDateTime.now().minusDays(2), 100L);
        Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 300L);
        Post awsPost = createPost("AWS 게시글", techBlog3, LocalDateTime.now(), 500L);
        postRepository.saveAll(List.of(kakaoPost, naverPost, awsPost));

        // When
        List<PostInfoDto> result = postRepository.findByCompanyNamesWithCursor(null, null, null, PageRequest.of(0, 10));

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(PostInfoDto::company)
                .containsExactlyInAnyOrder("카카오", "네이버", "AWS");
    }

    @Test
    @DisplayName("findByCompanyNamesWithCursor - companies 지정 시 해당 회사들 게시글만 조회")
    void findByCompanyNamesWithCursor_SpecificCompanies_ReturnsFiltered() {
        // Given: 다른 회사의 게시글 4개
        Post kakaoPost1 = createPost("카카오 게시글1", techBlog1, LocalDateTime.now().minusDays(2), 100L);
        Post kakaoPost2 = createPost("카카오 게시글2", techBlog1, LocalDateTime.now().minusDays(1), 200L);
        Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 300L);
        Post awsPost = createPost("AWS 게시글", techBlog3, LocalDateTime.now(), 500L);
        postRepository.saveAll(List.of(kakaoPost1, kakaoPost2, naverPost, awsPost));

        // When: companies = {"카카오", "네이버"}
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<String> companyNames = List.of("카카오", "네이버");
        List<PostInfoDto> result = postRepository.findByCompanyNamesWithCursor(companyNames, null, null,pageRequest);

        // Then: 카카오와 네이버 게시글만 반환
        assertThat(result).hasSize(3);
        assertThat(result).extracting(PostInfoDto::company)
                .containsOnly("카카오", "네이버")
                .doesNotContain("AWS");
    }

    @Test
    @DisplayName("발행일 내림차순으로 정렬되는지 확인")
    void findByCompanyNames_SortPublishedAtCheck() {
        // Given: 발행시각 다른 게시글 생성
        Post recentPost = createPost("최신 글", techBlog1, LocalDateTime.now(), 100L);
        Post oldPost = createPost("옛날 글", techBlog1, LocalDateTime.now().minusDays(5), 200L);
        postRepository.saveAll(List.of(recentPost, oldPost));

        // When
        List<PostInfoDto> result = postRepository.findByCompanyNamesWithCursor(null, null, null, PageRequest.of(0, 10));

        // Then
        assertThat(result)
                .extracting(PostInfoDto::title)
                .containsExactly("최신 글", "옛날 글");
    }

    @Test
    @DisplayName("발행일이 같을 경우 ID 내림차순으로 정렬되는지 확인")
    void findByCompanyNames_SortPublihedAtEqualsCheck() {
        // Given: 발행시각이 같은 게시글 생성
        LocalDateTime now = LocalDateTime.now();
        Post kakaoPost = createPost("카카오 게시글", techBlog1, now, 100L);
        Post naverPost = createPost("네이버 게시글", techBlog2, now, 300L);
        Post awsPost = createPost("AWS 게시글", techBlog3, now, 500L);
        postRepository.saveAll(List.of(kakaoPost, naverPost, awsPost));

        // When
        List<PostInfoDto> result = postRepository.findByCompanyNamesWithCursor(null, null, null, PageRequest.of(0, 10));

        // Then: insert 쿼리의 역순
        assertThat(result)
                .extracting("id")
                .containsExactly(awsPost.getId(), naverPost.getId(), kakaoPost.getId());
    }

    @Test
    @DisplayName("커서 기반 페이징 - 1페이지 조회 후 커서 이용해 2페이지 조회")
    void findByCompanyNames_CursorPaging() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<Post> posts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            posts.add(createPost("게시글" + i, techBlog1, now.plusHours(i), 10L));
        }
        postRepository.saveAll(posts);

        PageRequest pageRequest = PageRequest.of(0, 2);

        // When
        List<PostInfoDto> page1 = postRepository.findByCompanyNamesWithCursor(
                null, null, null, pageRequest
        );

        PostInfoDto lastPostOfPage1 = page1.get(1);

        List<PostInfoDto> page2 = postRepository.findByCompanyNamesWithCursor(
                null, lastPostOfPage1.publishedAt(), lastPostOfPage1.id(), pageRequest
        );

        // Then
        assertThat(page1).extracting("title")
                .containsExactly("게시글5", "게시글4");

        assertThat(page2).extracting("title")
                .containsExactly("게시글3", "게시글2");
    }

    @Test
    @DisplayName("findByIdWithTechBlog - JOIN하여 게시글 상세 정보 조회 성공")
    void findByIdWithTechBlog_Success_ReturnsPostDetailDto() {
        // Given: 게시글 생성
        Post post = createPost("테스트 게시글", techBlog1, LocalDateTime.now(), 100L);
        post = postRepository.save(post);

        // When: ID로 조회
        Optional<PostDetailDto> result = postRepository.findByIdWithTechBlog(post.getId());

        // Then: PostDetailDto 프로젝션 성공
        assertThat(result).isPresent();
        PostDetailDto dto = result.get();
        assertThat(dto.id()).isEqualTo(post.getId());
        assertThat(dto.title()).isEqualTo("테스트 게시글");
        assertThat(dto.company()).isEqualTo("카카오");
        assertThat(dto.viewCount()).isEqualTo(100L);
        assertThat(dto.keywords()).isNull(); // 키워드는 별도 조회
    }

    @Test
    @DisplayName("findByIdWithTechBlog - 존재하지 않는 ID 조회 시 Empty 반환")
    void findByIdWithTechBlog_NotFound_ReturnsEmpty() {
        // When: 존재하지 않는 ID 조회
        Optional<PostDetailDto> result = postRepository.findByIdWithTechBlog(99999L);

        // Then: Empty 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDistinctCompanies - 중복 없이 회사 목록 조회")
    void findDistinctCompanies_ReturnsUniqueCompanies() {
        // Given: 같은 회사의 게시글 여러 개
        Post kakaoPost1 = createPost("카카오 게시글1", techBlog1, LocalDateTime.now(), 100L);
        Post kakaoPost2 = createPost("카카오 게시글2", techBlog1, LocalDateTime.now(), 200L);
        Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 300L);
        postRepository.saveAll(List.of(kakaoPost1, kakaoPost2, naverPost));

        // When: 회사 목록 조회
        List<String> result = postRepository.findDistinctCompanies();

        // Then: 중복 없이 2개 회사 반환
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("카카오", "네이버");
    }

    @Test
    @DisplayName("findCompaniesWithDetails - 회사별 상세 정보 조회 성공")
    void findCompaniesWithDetails_Success() {
        // Given: 여러 회사의 게시글 생성
        Post kakaoPost1 = createPost("카카오 게시글1", techBlog1, LocalDate.now().atStartOfDay(), 100L);
        Post kakaoPost2 = createPost("카카오 게시글2", techBlog1, LocalDate.now().minusDays(1).atStartOfDay(), 200L);
        Post naverPost = createPost("네이버 게시글", techBlog2, LocalDate.now().minusDays(2).atStartOfDay(), 300L);
        postRepository.saveAll(List.of(kakaoPost1, kakaoPost2, naverPost));

        // When: 회사 상세 정보 조회
        List<CompanyDto> result = postRepository.findCompaniesWithDetails();

        // Then: 회사별로 집계된 정보 반환
        assertThat(result).hasSize(2);

        // 최신 발행일 기준으로 정렬되어야 함 (카카오가 더 최신)
        CompanyDto firstCompany = result.get(0);
        assertThat(firstCompany.company()).isEqualTo("카카오");
        assertThat(firstCompany.hasNewPost()).isTrue(); // 오늘 발행된 게시글 존재
        assertThat(firstCompany.logoUrl()).isNotNull();

        CompanyDto secondCompany = result.get(1);
        assertThat(secondCompany.company()).isEqualTo("네이버");
        assertThat(secondCompany.hasNewPost()).isFalse(); // 오늘 발행된 게시글 없음
    }

    @Test
    @DisplayName("findCompaniesWithDetails - 오늘 발행된 게시글 여부 정확히 판단")
    void findCompaniesWithDetails_HasNewPost_AccurateDetection() {
        // Given: 오늘과 어제 게시글
        Post todayPost = createPost("오늘 게시글", techBlog1, LocalDate.now().atTime(14, 30), 100L);
        Post yesterdayPost = createPost("어제 게시글", techBlog2, LocalDate.now().minusDays(1).atStartOfDay(), 200L);
        postRepository.saveAll(List.of(todayPost, yesterdayPost));

        // When: 회사 상세 정보 조회
        List<CompanyDto> result = postRepository.findCompaniesWithDetails();

        // Then: 오늘 게시글 여부 정확히 판단
        CompanyDto kakaoCompany = result.stream()
                .filter(c -> c.company().equals("카카오"))
                .findFirst()
                .orElseThrow();
        assertThat(kakaoCompany.hasNewPost()).isTrue();

        CompanyDto naverCompany = result.stream()
                .filter(c -> c.company().equals("네이버"))
                .findFirst()
                .orElseThrow();
        assertThat(naverCompany.hasNewPost()).isFalse();
    }

    @Test
    @DisplayName("findCompaniesWithDetails - 최신 발행일 기준으로 정렬")
    void findCompaniesWithDetails_OrderByLatestPublishedAtDesc() {
        // Given: 발행일이 다른 게시글들
        Post oldPost = createPost("오래된 게시글", techBlog1, LocalDate.now().minusDays(10).atStartOfDay(), 100L);
        Post recentPost = createPost("최근 게시글", techBlog2, LocalDate.now().minusDays(1).atStartOfDay(), 200L);
        postRepository.saveAll(List.of(oldPost, recentPost));

        // When: 회사 상세 정보 조회
        List<CompanyDto> result = postRepository.findCompaniesWithDetails();

        // Then: 최신 발행일 기준 정렬 (네이버가 먼저)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).company()).isEqualTo("네이버");
        assertThat(result.get(1).company()).isEqualTo("카카오");
    }

    @Test
    @DisplayName("findCompaniesWithDetails - 게시글이 없으면 빈 리스트 반환")
    void findCompaniesWithDetails_NoPosts_ReturnsEmptyList() {
        // Given: 게시글 없음

        // When: 회사 상세 정보 조회
        List<CompanyDto> result = postRepository.findCompaniesWithDetails();

        // Then: 빈 리스트 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("커서 페이징 - size+1 조회하여 hasNext 판단 가능")
    void cursorPaging_SizePlusOne_CanDetermineHasNext() {
        // Given: 게시글 5개
        for (int i = 1; i <= 5; i++) {
            Post post = createPost("게시글" + i, techBlog1, LocalDateTime.now().minusDays(i), (long) (i * 100));
            postRepository.save(post);
        }

        // When: size = 3으로 조회 (실제로는 4개 조회)
        PageRequest pageRequest = PageRequest.of(0, 4);
        List<PostInfoDto> result = postRepository.findRecentPostsWithCursor(null, pageRequest);

        // Then: 4개 조회되면 hasNext = true
        assertThat(result).hasSize(4);
        boolean hasNext = result.size() > 3;
        assertThat(hasNext).isTrue();
    }

    @Test
    @DisplayName("findRecentPostsWithCursorV2 - publishedAt과 id로 커서 페이징")
    void findRecentPostsWithCursorV2_CursorPagingWithPublishedAtAndId() {
        // Given: 같은 publishedAt을 가진 게시글 3개
        LocalDateTime now = LocalDateTime.now();
        Post post1 = createPost("게시글1", techBlog1, now, 100L);
        Post post2 = createPost("게시글2", techBlog1, now, 200L);
        Post post3 = createPost("게시글3", techBlog1, now, 300L);
        postRepository.saveAll(List.of(post1, post2, post3));

        // When: 첫 페이지 조회
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<PostInfoDto> page1 = postRepository.findRecentPostsWithCursorV2(null, null, pageRequest);

        // Then: publishedAt 같으면 id 내림차순으로 정렬
        assertThat(page1).hasSize(3);
        assertThat(page1.get(0).id()).isGreaterThan(page1.get(1).id());
        assertThat(page1.get(1).id()).isGreaterThan(page1.get(2).id());
    }

    @Test
    @DisplayName("findRecentPostsWithCursorV2 - 커서 기반 다음 페이지 조회")
    void findRecentPostsWithCursorV2_NextPageWithCursor() {
        // Given: 발행일이 다른 게시글 5개
        LocalDateTime now = LocalDateTime.now();
        Post post1 = createPost("게시글1", techBlog1, now.minusDays(1), 100L);
        Post post2 = createPost("게시글2", techBlog1, now.minusDays(2), 200L);
        Post post3 = createPost("게시글3", techBlog1, now.minusDays(3), 300L);
        Post post4 = createPost("게시글4", techBlog1, now.minusDays(4), 400L);
        Post post5 = createPost("게시글5", techBlog1, now.minusDays(5), 500L);
        postRepository.saveAll(List.of(post1, post2, post3, post4, post5));

        // When: 첫 페이지 조회 후 두 번째 페이지 조회
        PageRequest pageRequest = PageRequest.of(0, 2);
        List<PostInfoDto> page1 = postRepository.findRecentPostsWithCursorV2(null, null, pageRequest);

        PostInfoDto lastPost = page1.get(1);
        List<PostInfoDto> page2 = postRepository.findRecentPostsWithCursorV2(
                lastPost.publishedAt(), lastPost.id(), pageRequest
        );

        // Then: 커서 이후 게시글만 반환
        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
        assertThat(page2.get(0).publishedAt()).isBefore(lastPost.publishedAt());
    }

    @Test
    @DisplayName("findPopularPostsWithCursorV2 - viewCount와 id로 커서 페이징")
    void findPopularPostsWithCursorV2_CursorPagingWithViewCountAndId() {
        // Given: 조회수가 다른 게시글 3개
        Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(1), 500L);
        Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(2), 300L);
        Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(3), 100L);
        postRepository.saveAll(List.of(post1, post2, post3));

        // When: 첫 페이지 조회
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<PostInfoDto> result = postRepository.findPopularPostsWithCursorV2(null, null, pageRequest);

        // Then: viewCount 내림차순으로 정렬 (500 > 300 > 100)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).viewCount()).isEqualTo(500L);
        assertThat(result.get(1).viewCount()).isEqualTo(300L);
        assertThat(result.get(2).viewCount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("findPopularPostsWithCursorV2 - 같은 viewCount일 때 id로 정렬")
    void findPopularPostsWithCursorV2_SameViewCount_OrderById() {
        // Given: 같은 조회수를 가진 게시글 3개
        Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(1), 500L);
        Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(2), 500L);
        Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(3), 500L);
        postRepository.saveAll(List.of(post1, post2, post3));

        // When: 조회
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<PostInfoDto> result = postRepository.findPopularPostsWithCursorV2(null, null, pageRequest);

        // Then: viewCount 같으면 id 내림차순
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isGreaterThan(result.get(1).id());
        assertThat(result.get(1).id()).isGreaterThan(result.get(2).id());
    }

    @Test
    @DisplayName("findPopularPostsWithCursorV2 - 커서 기반 다음 페이지 조회")
    void findPopularPostsWithCursorV2_NextPageWithCursor() {
        // Given: 조회수가 다른 게시글 5개
        Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(1), 500L);
        Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(2), 400L);
        Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(3), 300L);
        Post post4 = createPost("게시글4", techBlog1, LocalDateTime.now().minusDays(4), 200L);
        Post post5 = createPost("게시글5", techBlog1, LocalDateTime.now().minusDays(5), 100L);
        postRepository.saveAll(List.of(post1, post2, post3, post4, post5));

        // When: 첫 페이지 조회 후 두 번째 페이지 조회
        PageRequest pageRequest = PageRequest.of(0, 2);
        List<PostInfoDto> page1 = postRepository.findPopularPostsWithCursorV2(null, null, pageRequest);

        PostInfoDto lastPost = page1.get(1);
        List<PostInfoDto> page2 = postRepository.findPopularPostsWithCursorV2(
                lastPost.viewCount().intValue(), lastPost.id(), pageRequest
        );

        // Then: 커서 이후 게시글만 반환
        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
        assertThat(page2.get(0).viewCount()).isLessThanOrEqualTo(lastPost.viewCount());
    }

    // 헬퍼 메서드
    private Post createPost(String title, TechBlog techBlog, LocalDateTime publishedAt, Long viewCount) {
        Post post = Post.builder()
                .title(title)
                .fullContent("<p>" + title + " 내용</p>")
                .plainContent(title + " 내용")
                .company(techBlog.getCompanyName())
                .url("https://test.com/" + title)
                .logoUrl("https://test.com/logo.png")
                .thumbnailUrl("https://test.com/thumb.png")
                .publishedAt(publishedAt)
                .crawledAt(LocalDateTime.now())
                .techBlog(techBlog)
                .build();

        // viewCount 설정 (reflection 또는 여러 번 증가)
        for (int i = 0; i < viewCount; i++) {
            post.incrementViewCount();
        }

        return post;
    }
}
