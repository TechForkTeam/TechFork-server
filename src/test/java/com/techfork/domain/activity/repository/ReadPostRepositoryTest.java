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
}
