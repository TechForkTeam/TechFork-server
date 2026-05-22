package com.techfork.post.infrastructure;

import com.techfork.post.application.dto.CompanyDto;
import com.techfork.post.application.dto.PostDetailDto;
import com.techfork.post.application.dto.PostInfoDto;
import com.techfork.post.domain.Post;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Autowired
    private EntityManager entityManager;

    private TechBlog techBlog1;
    private TechBlog techBlog2;
    private TechBlog techBlog3;

    @BeforeEach
    void setUp() {
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

    @Nested
    @DisplayName("조회수 증가")
    class IncrementViewCount {

        @Test
        @DisplayName("조회수를 1 증가시킨다")
        void incrementViewCount_IncreasesViewCount() {
            Post post = postRepository.save(createPost("조회수 증가 대상", techBlog1, LocalDateTime.now(), 0L));

            int updatedCount = postRepository.incrementViewCount(post.getId());

            entityManager.clear();

            Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updatedCount).isEqualTo(1);
            assertThat(updatedPost.getViewCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("여러 번 호출하면 누적 증가한다")
        void incrementViewCount_MultipleCalls_AccumulatesViewCount() {
            Post post = postRepository.save(createPost("누적 조회수 대상", techBlog1, LocalDateTime.now(), 5L));

            postRepository.incrementViewCount(post.getId());
            postRepository.incrementViewCount(post.getId());

            entityManager.clear();

            Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
            assertThat(updatedPost.getViewCount()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("인기 게시글 조회 V1")
    class FindPopularPostsWithCursor {

        @Test
        @DisplayName("lastPostId가 null이면 첫 페이지를 조회한다")
        void findPopularPostsWithCursor_FirstPage_OrderByViewCountDesc() {
            Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(3), 100L);
            Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(2), 500L);
            Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(1), 300L);
            postRepository.saveAll(List.of(post1, post2, post3));

            List<PostInfoDto> result = postRepository.findPopularPostsWithCursor(null, PageRequest.of(0, 10));

            assertThat(result).hasSize(3);
            assertThat(result.get(0).viewCount()).isEqualTo(500L);
            assertThat(result.get(1).viewCount()).isEqualTo(300L);
            assertThat(result.get(2).viewCount()).isEqualTo(100L);
        }

        @Test
        @DisplayName("lastPostId 지정 시 해당 ID 이후 게시글만 조회한다")
        void findPopularPostsWithCursor_NextPage_FilterByLastPostId() {
            Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(5), 500L);
            Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(4), 400L);
            Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(3), 300L);
            Post post4 = createPost("게시글4", techBlog1, LocalDateTime.now().minusDays(2), 200L);
            Post post5 = createPost("게시글5", techBlog1, LocalDateTime.now().minusDays(1), 100L);
            postRepository.saveAll(List.of(post1, post2, post3, post4, post5));

            List<PostInfoDto> result = postRepository.findPopularPostsWithCursor(post3.getId(), PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(dto -> dto.id() < post3.getId());
        }
    }

    @Nested
    @DisplayName("인기 게시글 조회 V2")
    class FindPopularPostsWithCursorV2 {

        @Test
        @DisplayName("viewCount와 id로 커서 페이징한다")
        void findPopularPostsWithCursorV2_CursorPagingWithViewCountAndId() {
            Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(1), 500L);
            Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(2), 300L);
            Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(3), 100L);
            postRepository.saveAll(List.of(post1, post2, post3));

            List<PostInfoDto> result = postRepository.findPopularPostsWithCursorV2(null, null, PageRequest.of(0, 10));

            assertThat(result).hasSize(3);
            assertThat(result.get(0).viewCount()).isEqualTo(500L);
            assertThat(result.get(1).viewCount()).isEqualTo(300L);
            assertThat(result.get(2).viewCount()).isEqualTo(100L);
        }

        @Test
        @DisplayName("같은 viewCount일 때 id로 정렬한다")
        void findPopularPostsWithCursorV2_SameViewCount_OrderById() {
            Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(1), 500L);
            Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(2), 500L);
            Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(3), 500L);
            postRepository.saveAll(List.of(post1, post2, post3));

            List<PostInfoDto> result = postRepository.findPopularPostsWithCursorV2(null, null, PageRequest.of(0, 10));

            assertThat(result).hasSize(3);
            assertThat(result.get(0).id()).isGreaterThan(result.get(1).id());
            assertThat(result.get(1).id()).isGreaterThan(result.get(2).id());
        }

        @Test
        @DisplayName("커서 기반으로 다음 페이지를 조회한다")
        void findPopularPostsWithCursorV2_NextPageWithCursor() {
            Post post1 = createPost("게시글1", techBlog1, LocalDateTime.now().minusDays(1), 500L);
            Post post2 = createPost("게시글2", techBlog1, LocalDateTime.now().minusDays(2), 400L);
            Post post3 = createPost("게시글3", techBlog1, LocalDateTime.now().minusDays(3), 300L);
            Post post4 = createPost("게시글4", techBlog1, LocalDateTime.now().minusDays(4), 200L);
            Post post5 = createPost("게시글5", techBlog1, LocalDateTime.now().minusDays(5), 100L);
            postRepository.saveAll(List.of(post1, post2, post3, post4, post5));

            PageRequest pageRequest = PageRequest.of(0, 2);
            List<PostInfoDto> page1 = postRepository.findPopularPostsWithCursorV2(null, null, pageRequest);
            PostInfoDto lastPost = page1.get(1);
            List<PostInfoDto> page2 = postRepository.findPopularPostsWithCursorV2(
                    lastPost.viewCount().intValue(),
                    lastPost.id(),
                    pageRequest
            );

            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(2);
            assertThat(page2.get(0).viewCount()).isLessThanOrEqualTo(lastPost.viewCount());
        }
    }

    @Nested
    @DisplayName("최신 게시글 조회 V1")
    class FindRecentPostsWithCursor {

        @Test
        @DisplayName("최신 발행일 순으로 정렬한다")
        void findRecentPostsWithCursor_OrderByPublishedAtDesc() {
            LocalDateTime now = LocalDateTime.now();
            Post post1 = createPost("게시글1", techBlog1, now.minusDays(3), 100L);
            Post post2 = createPost("게시글2", techBlog1, now.minusDays(1), 200L);
            Post post3 = createPost("게시글3", techBlog1, now.minusDays(2), 300L);
            postRepository.saveAll(List.of(post1, post2, post3));

            List<PostInfoDto> result = postRepository.findRecentPostsWithCursor(null, PageRequest.of(0, 10));

            assertThat(result).hasSize(3);
            assertThat(result.get(0).publishedAt()).isAfter(result.get(1).publishedAt());
            assertThat(result.get(1).publishedAt()).isAfter(result.get(2).publishedAt());
        }

        @Test
        @DisplayName("size+1 조회로 hasNext 판단이 가능하다")
        void cursorPaging_SizePlusOne_CanDetermineHasNext() {
            for (int i = 1; i <= 5; i++) {
                Post post = createPost("게시글" + i, techBlog1, LocalDateTime.now().minusDays(i), (long) (i * 100));
                postRepository.save(post);
            }

            List<PostInfoDto> result = postRepository.findRecentPostsWithCursor(null, PageRequest.of(0, 4));

            assertThat(result).hasSize(4);
            boolean hasNext = result.size() > 3;
            assertThat(hasNext).isTrue();
        }
    }

    @Nested
    @DisplayName("최신 게시글 조회 V2")
    class FindRecentPostsWithCursorV2 {

        @Test
        @DisplayName("publishedAt과 id로 커서 페이징한다")
        void findRecentPostsWithCursorV2_CursorPagingWithPublishedAtAndId() {
            LocalDateTime now = LocalDateTime.now();
            Post post1 = createPost("게시글1", techBlog1, now, 100L);
            Post post2 = createPost("게시글2", techBlog1, now, 200L);
            Post post3 = createPost("게시글3", techBlog1, now, 300L);
            postRepository.saveAll(List.of(post1, post2, post3));

            List<PostInfoDto> page1 = postRepository.findRecentPostsWithCursorV2(null, null, PageRequest.of(0, 10));

            assertThat(page1).hasSize(3);
            assertThat(page1.get(0).id()).isGreaterThan(page1.get(1).id());
            assertThat(page1.get(1).id()).isGreaterThan(page1.get(2).id());
        }

        @Test
        @DisplayName("커서 기반으로 다음 페이지를 조회한다")
        void findRecentPostsWithCursorV2_NextPageWithCursor() {
            LocalDateTime now = LocalDateTime.now();
            Post post1 = createPost("게시글1", techBlog1, now.minusDays(1), 100L);
            Post post2 = createPost("게시글2", techBlog1, now.minusDays(2), 200L);
            Post post3 = createPost("게시글3", techBlog1, now.minusDays(3), 300L);
            Post post4 = createPost("게시글4", techBlog1, now.minusDays(4), 400L);
            Post post5 = createPost("게시글5", techBlog1, now.minusDays(5), 500L);
            postRepository.saveAll(List.of(post1, post2, post3, post4, post5));

            PageRequest pageRequest = PageRequest.of(0, 2);
            List<PostInfoDto> page1 = postRepository.findRecentPostsWithCursorV2(null, null, pageRequest);
            PostInfoDto lastPost = page1.get(1);
            List<PostInfoDto> page2 = postRepository.findRecentPostsWithCursorV2(
                    lastPost.publishedAt(),
                    lastPost.id(),
                    pageRequest
            );

            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(2);
            assertThat(page2.get(0).publishedAt()).isBefore(lastPost.publishedAt());
        }
    }

    @Nested
    @DisplayName("회사별 게시글 조회 V1")
    class FindByCompanyWithCursor {

        @Test
        @DisplayName("company가 null이면 모든 게시글을 조회한다")
        void findByCompanyWithCursor_NullCompany_ReturnsAll() {
            Post kakaoPost = createPost("카카오 게시글", techBlog1, LocalDateTime.now(), 100L);
            Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 200L);
            postRepository.saveAll(List.of(kakaoPost, naverPost));

            List<PostInfoDto> result = postRepository.findByCompanyWithCursor(null, null, PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(PostInfoDto::company)
                    .containsExactlyInAnyOrder("카카오", "네이버");
        }

        @Test
        @DisplayName("company 지정 시 해당 회사 게시글만 조회한다")
        void findByCompanyWithCursor_SpecificCompany_ReturnsFiltered() {
            Post kakaoPost1 = createPost("카카오 게시글1", techBlog1, LocalDateTime.now().minusDays(2), 100L);
            Post kakaoPost2 = createPost("카카오 게시글2", techBlog1, LocalDateTime.now().minusDays(1), 200L);
            Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 300L);
            postRepository.saveAll(List.of(kakaoPost1, kakaoPost2, naverPost));

            List<PostInfoDto> result = postRepository.findByCompanyWithCursor("카카오", null, PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(dto -> dto.company().equals("카카오"));
        }
    }

    @Nested
    @DisplayName("회사 목록 기반 게시글 조회 V2")
    class FindByCompanyNamesWithCursor {

        @Test
        @DisplayName("companies가 null이면 모든 회사 게시글을 조회한다")
        void findByCompanyNames_NullCompanies_ReturnsAll() {
            Post kakaoPost = createPost("카카오 게시글", techBlog1, LocalDateTime.now().minusDays(2), 100L);
            Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 300L);
            Post awsPost = createPost("AWS 게시글", techBlog3, LocalDateTime.now(), 500L);
            postRepository.saveAll(List.of(kakaoPost, naverPost, awsPost));

            List<PostInfoDto> result = postRepository.findByCompanyNamesWithCursor(null, null, null, PageRequest.of(0, 10));

            assertThat(result).hasSize(3);
            assertThat(result).extracting(PostInfoDto::company)
                    .containsExactlyInAnyOrder("카카오", "네이버", "AWS");
        }

        @Test
        @DisplayName("companies 지정 시 해당 회사들 게시글만 조회한다")
        void findByCompanyNamesWithCursor_SpecificCompanies_ReturnsFiltered() {
            Post kakaoPost1 = createPost("카카오 게시글1", techBlog1, LocalDateTime.now().minusDays(2), 100L);
            Post kakaoPost2 = createPost("카카오 게시글2", techBlog1, LocalDateTime.now().minusDays(1), 200L);
            Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 300L);
            Post awsPost = createPost("AWS 게시글", techBlog3, LocalDateTime.now(), 500L);
            postRepository.saveAll(List.of(kakaoPost1, kakaoPost2, naverPost, awsPost));

            List<PostInfoDto> result = postRepository.findByCompanyNamesWithCursor(
                    List.of("카카오", "네이버"),
                    null,
                    null,
                    PageRequest.of(0, 10)
            );

            assertThat(result).hasSize(3);
            assertThat(result).extracting(PostInfoDto::company)
                    .containsOnly("카카오", "네이버")
                    .doesNotContain("AWS");
        }

        @Test
        @DisplayName("발행일 내림차순으로 정렬한다")
        void findByCompanyNames_SortPublishedAtCheck() {
            Post recentPost = createPost("최신 글", techBlog1, LocalDateTime.now(), 100L);
            Post oldPost = createPost("옛날 글", techBlog1, LocalDateTime.now().minusDays(5), 200L);
            postRepository.saveAll(List.of(recentPost, oldPost));

            List<PostInfoDto> result = postRepository.findByCompanyNamesWithCursor(null, null, null, PageRequest.of(0, 10));

            assertThat(result)
                    .extracting(PostInfoDto::title)
                    .containsExactly("최신 글", "옛날 글");
        }

        @Test
        @DisplayName("발행일이 같으면 ID 내림차순으로 정렬한다")
        void findByCompanyNames_SortPublihedAtEqualsCheck() {
            LocalDateTime now = LocalDateTime.now();
            Post kakaoPost = createPost("카카오 게시글", techBlog1, now, 100L);
            Post naverPost = createPost("네이버 게시글", techBlog2, now, 300L);
            Post awsPost = createPost("AWS 게시글", techBlog3, now, 500L);
            postRepository.saveAll(List.of(kakaoPost, naverPost, awsPost));

            List<PostInfoDto> result = postRepository.findByCompanyNamesWithCursor(null, null, null, PageRequest.of(0, 10));

            assertThat(result)
                    .extracting("id")
                    .containsExactly(awsPost.getId(), naverPost.getId(), kakaoPost.getId());
        }

        @Test
        @DisplayName("커서 기반으로 다음 페이지를 조회한다")
        void findByCompanyNames_CursorPaging() {
            LocalDateTime now = LocalDateTime.now();
            List<Post> posts = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                posts.add(createPost("게시글" + i, techBlog1, now.plusHours(i), 10L));
            }
            postRepository.saveAll(posts);

            PageRequest pageRequest = PageRequest.of(0, 2);
            List<PostInfoDto> page1 = postRepository.findByCompanyNamesWithCursor(null, null, null, pageRequest);
            PostInfoDto lastPostOfPage1 = page1.get(1);
            List<PostInfoDto> page2 = postRepository.findByCompanyNamesWithCursor(
                    null,
                    lastPostOfPage1.publishedAt(),
                    lastPostOfPage1.id(),
                    pageRequest
            );

            assertThat(page1).extracting("title")
                    .containsExactly("게시글5", "게시글4");
            assertThat(page2).extracting("title")
                    .containsExactly("게시글3", "게시글2");
        }
    }

    @Nested
    @DisplayName("게시글 상세 조회")
    class FindByIdWithTechBlog {

        @Test
        @DisplayName("JOIN하여 게시글 상세 정보 조회에 성공한다")
        void findByIdWithTechBlog_Success_ReturnsPostDetailDto() {
            Post post = postRepository.save(createPost("테스트 게시글", techBlog1, LocalDateTime.now(), 100L));

            Optional<PostDetailDto> result = postRepository.findByIdWithTechBlog(post.getId());

            assertThat(result).isPresent();
            PostDetailDto dto = result.get();
            assertThat(dto.id()).isEqualTo(post.getId());
            assertThat(dto.title()).isEqualTo("테스트 게시글");
            assertThat(dto.company()).isEqualTo("카카오");
            assertThat(dto.viewCount()).isEqualTo(100L);
            assertThat(dto.keywords()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 Empty를 반환한다")
        void findByIdWithTechBlog_NotFound_ReturnsEmpty() {
            Optional<PostDetailDto> result = postRepository.findByIdWithTechBlog(99999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("회사 목록 조회")
    class FindDistinctCompanies {

        @Test
        @DisplayName("중복 없이 회사 목록을 조회한다")
        void findDistinctCompanies_ReturnsUniqueCompanies() {
            Post kakaoPost1 = createPost("카카오 게시글1", techBlog1, LocalDateTime.now(), 100L);
            Post kakaoPost2 = createPost("카카오 게시글2", techBlog1, LocalDateTime.now(), 200L);
            Post naverPost = createPost("네이버 게시글", techBlog2, LocalDateTime.now(), 300L);
            postRepository.saveAll(List.of(kakaoPost1, kakaoPost2, naverPost));

            List<String> result = postRepository.findDistinctCompanies();

            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder("카카오", "네이버");
        }
    }

    @Nested
    @DisplayName("회사 상세 정보 조회")
    class FindCompaniesWithDetails {

        @Test
        @DisplayName("회사별 상세 정보 조회에 성공한다")
        void findCompaniesWithDetails_Success() {
            Post kakaoPost1 = createPost("카카오 게시글1", techBlog1, LocalDate.now().atStartOfDay(), 100L);
            Post kakaoPost2 = createPost("카카오 게시글2", techBlog1, LocalDate.now().minusDays(1).atStartOfDay(), 200L);
            Post naverPost = createPost("네이버 게시글", techBlog2, LocalDate.now().minusDays(2).atStartOfDay(), 300L);
            postRepository.saveAll(List.of(kakaoPost1, kakaoPost2, naverPost));

            List<CompanyDto> result = postRepository.findCompaniesWithDetails();

            assertThat(result).hasSize(2);

            CompanyDto firstCompany = result.get(0);
            assertThat(firstCompany.company()).isEqualTo("카카오");
            assertThat(firstCompany.hasNewPost()).isTrue();
            assertThat(firstCompany.logoUrl()).isNotNull();

            CompanyDto secondCompany = result.get(1);
            assertThat(secondCompany.company()).isEqualTo("네이버");
            assertThat(secondCompany.hasNewPost()).isFalse();
        }

        @Test
        @DisplayName("오늘 발행된 게시글 여부를 정확히 판단한다")
        void findCompaniesWithDetails_HasNewPost_AccurateDetection() {
            Post todayPost = createPost("오늘 게시글", techBlog1, LocalDate.now().atTime(14, 30), 100L);
            Post yesterdayPost = createPost("어제 게시글", techBlog2, LocalDate.now().minusDays(1).atStartOfDay(), 200L);
            postRepository.saveAll(List.of(todayPost, yesterdayPost));

            List<CompanyDto> result = postRepository.findCompaniesWithDetails();

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
        @DisplayName("최신 발행일 기준으로 정렬한다")
        void findCompaniesWithDetails_OrderByLatestPublishedAtDesc() {
            Post oldPost = createPost("오래된 게시글", techBlog1, LocalDate.now().minusDays(10).atStartOfDay(), 100L);
            Post recentPost = createPost("최근 게시글", techBlog2, LocalDate.now().minusDays(1).atStartOfDay(), 200L);
            postRepository.saveAll(List.of(oldPost, recentPost));

            List<CompanyDto> result = postRepository.findCompaniesWithDetails();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).company()).isEqualTo("네이버");
            assertThat(result.get(1).company()).isEqualTo("카카오");
        }

        @Test
        @DisplayName("게시글이 없으면 빈 리스트를 반환한다")
        void findCompaniesWithDetails_NoPosts_ReturnsEmptyList() {
            List<CompanyDto> result = postRepository.findCompaniesWithDetails();

            assertThat(result).isEmpty();
        }
    }

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

        ReflectionTestUtils.setField(post, "viewCount", viewCount);

        return post;
    }
}
