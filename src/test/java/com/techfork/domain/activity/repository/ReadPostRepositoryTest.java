package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReadPostRepositoryTest {

    @Autowired
    private ReadPostRepository readPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    private User testUser;
    private Post testPost1;
    private Post testPost2;
    private TechBlog testBlog;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
        testUser = userRepository.save(testUser);

        testBlog = TechBlog.builder()
                .companyName("테스트회사")
                .blogUrl("https://test.com")
                .rssUrl("https://test.com/rss")
                .build();
        testBlog = techBlogRepository.save(testBlog);

        testPost1 = Post.builder()
                .title("테스트 게시글 1")
                .fullContent("전체 내용")
                .plainContent("내용")
                .company("테스트회사")
                .url("https://test.com/post/1")
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost1 = postRepository.save(testPost1);

        testPost2 = Post.builder()
                .title("테스트 게시글 2")
                .fullContent("전체 내용 2")
                .plainContent("내용 2")
                .company("테스트회사")
                .url("https://test.com/post/2")
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost2 = postRepository.save(testPost2);
    }

    @AfterEach
    void tearDown() {
        readPostRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("최근 읽은 게시글 조회 - readDurationSeconds 10초 초과 필터링")
    void findRecentReadPostsByUserIdWithMinDuration() {
        // Given
        ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(2), 5);
        ReadPost readPost2 = ReadPost.create(testUser, testPost2, LocalDateTime.now(), null);
        readPostRepository.saveAll(List.of(readPost1, readPost2));

        PageRequest pageRequest = PageRequest.of(0, 10);

        // When
        List<ReadPost> result = readPostRepository.findRecentReadPostsByUserIdWithMinDuration(testUser.getId(), pageRequest);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReadDurationSeconds()).isNull();
    }

    @Test
    @DisplayName("같은 게시글 중복 저장 가능 - unique 제약조건 없음")
    void saveDuplicateReadPost_Success() {
        // Given
        ReadPost readPost1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(1), 100);
        ReadPost readPost2 = ReadPost.create(testUser, testPost1, LocalDateTime.now(), 200);

        // When
        readPostRepository.save(readPost1);
        readPostRepository.save(readPost2);
        readPostRepository.flush();

        // Then
        List<ReadPost> all = readPostRepository.findAll();
        assertThat(all).hasSize(2);
        assertThat(all).allMatch(rp -> rp.getPost().getId().equals(testPost1.getId()));
        assertThat(all).allMatch(rp -> rp.getUser().getId().equals(testUser.getId()));
    }

    @Test
    @DisplayName("읽은 게시글 목록 조회 - 동일 포스트 중복 제거 및 최신순 정렬 확인")
    void findReadPostsWithCursor_DeduplicateByPostId_Success() {
        // Given
        // 동일한 포스트(testPost1)를 두 번 읽음
        ReadPost read1 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(2), 100);
        ReadPost read2 = ReadPost.create(testUser, testPost1, LocalDateTime.now().minusHours(1), 100);
        // 다른 포스트(testPost2)를 읽음
        ReadPost read3 = ReadPost.create(testUser, testPost2, LocalDateTime.now(), 100);

        readPostRepository.saveAll(List.of(read1, read2, read3));

        PageRequest pageRequest = PageRequest.of(0, 10);

        // When
        List<com.techfork.domain.activity.dto.ReadPostDto> result = readPostRepository.findReadPostsWithCursor(testUser.getId(), null, pageRequest);

        // Then
        // 1. 중복 제거 확인: 총 3개의 데이터 중 포스트 종류는 2개이므로 결과는 2개여야 함
        assertThat(result).hasSize(2);

        // 2. 최신 데이터 확인: testPost1에 대해서는 나중에 읽은 read2의 ID가 포함되어야 함
        assertThat(result).extracting(com.techfork.domain.activity.dto.ReadPostDto::postId)
                .containsExactlyInAnyOrder(testPost1.getId(), testPost2.getId());

        // ID 기준으로 내림차순 정렬되었는지 확인 (read3 -> read2 순서)
        // saveAll 후 ID를 가져오기 위해 리포지토리에서 다시 조회하거나, 순서만 확인
        assertThat(result.get(0).postId()).isEqualTo(testPost2.getId()); // read3
        assertThat(result.get(1).postId()).isEqualTo(testPost1.getId()); // read2
    }
}
