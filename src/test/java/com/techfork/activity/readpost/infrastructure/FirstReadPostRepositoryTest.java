package com.techfork.activity.readpost.infrastructure;

import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.useraccount.entity.User;
import com.techfork.useraccount.enums.SocialType;
import com.techfork.useraccount.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class FirstReadPostRepositoryTest {

    @Autowired
    private FirstReadPostRepository firstReadPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
        testUser = userRepository.save(testUser);

        TechBlog testBlog = TechBlog.builder()
                .companyName("테스트회사")
                .blogUrl("https://test.com")
                .rssUrl("https://test.com/rss")
                .build();
        testBlog = techBlogRepository.save(testBlog);

        testPost = Post.builder()
                .title("테스트 게시글")
                .fullContent("전체 내용")
                .plainContent("내용")
                .company("테스트회사")
                .url("https://test.com/post/1")
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost = postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        firstReadPostRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("첫 읽기 마킹은 같은 user/post 조합에서 한 번만 성공한다")
    void markFirstRead_SuccessOnlyOncePerUserPostPair() {
        LocalDateTime firstReadAt = LocalDateTime.of(2026, 5, 8, 9, 0);
        LocalDateTime secondReadAt = LocalDateTime.of(2026, 5, 8, 9, 5);

        boolean firstMarked = firstReadPostRepository.markFirstRead(testUser.getId(), testPost.getId(), firstReadAt);
        boolean secondMarked = firstReadPostRepository.markFirstRead(testUser.getId(), testPost.getId(), secondReadAt);

        List<com.techfork.activity.readpost.domain.FirstReadPost> savedFirstReads = firstReadPostRepository.findAll();

        assertThat(firstMarked).isTrue();
        assertThat(secondMarked).isFalse();
        assertThat(savedFirstReads).hasSize(1);
        assertThat(savedFirstReads.get(0).getFirstReadAt()).isEqualTo(firstReadAt);
        assertThat(savedFirstReads.get(0).getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedFirstReads.get(0).getPost().getId()).isEqualTo(testPost.getId());
    }

    @Test
    @DisplayName("중복이 아닌 무결성 오류는 그대로 전파한다")
    void markFirstRead_Throws_WhenIntegrityViolationIsNotDuplicateKey() {
        LocalDateTime readAt = LocalDateTime.of(2026, 5, 8, 10, 0);

        assertThatThrownBy(() -> firstReadPostRepository.markFirstRead(testUser.getId(), 999999L, readAt))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(firstReadPostRepository.findAll()).isEmpty();
    }
}
