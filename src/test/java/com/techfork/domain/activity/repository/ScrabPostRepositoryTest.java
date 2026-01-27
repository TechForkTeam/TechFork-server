package com.techfork.domain.activity.repository;

import com.techfork.domain.activity.dto.BookmarkDto;
import com.techfork.domain.activity.entity.ScrabPost;
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
class ScrabPostRepositoryTest {

    @Autowired
    private ScrabPostRepository scrabPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    private User testUser;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;
    private TechBlog testBlog;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
        testUser = userRepository.save(testUser);

        testBlog = TechBlog.builder()
                .companyName("테스트회사")
                .blogUrl("https://test.com")
                .rssUrl("https://test.com/rss")
                .logoUrl("https://test.com/logo.png")
                .build();
        testBlog = techBlogRepository.save(testBlog);

        testPost1 = Post.builder()
                .title("게시글 1")
                .fullContent("내용 1")
                .plainContent("내용 1")
                .company("테스트회사")
                .url("https://test.com/post/1")
                .publishedAt(LocalDateTime.now().minusDays(3))
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost1 = postRepository.save(testPost1);

        testPost2 = Post.builder()
                .title("게시글 2")
                .fullContent("내용 2")
                .plainContent("내용 2")
                .company("테스트회사")
                .url("https://test.com/post/2")
                .publishedAt(LocalDateTime.now().minusDays(2))
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost2 = postRepository.save(testPost2);

        testPost3 = Post.builder()
                .title("게시글 3")
                .fullContent("내용 3")
                .plainContent("내용 3")
                .company("테스트회사")
                .url("https://test.com/post/3")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .crawledAt(LocalDateTime.now())
                .techBlog(testBlog)
                .build();
        testPost3 = postRepository.save(testPost3);
    }

    @AfterEach
    void tearDown() {
        scrabPostRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("북마크 목록 조회 - 커서 기반 페이징")
    void findBookmarksWithCursor() {
        // Given
        ScrabPost scrab1 = ScrabPost.create(testUser, testPost1, LocalDateTime.now().minusHours(3));
        ScrabPost scrab2 = ScrabPost.create(testUser, testPost2, LocalDateTime.now().minusHours(2));
        ScrabPost scrab3 = ScrabPost.create(testUser, testPost3, LocalDateTime.now().minusHours(1));
        scrab1 = scrabPostRepository.save(scrab1);
        scrab2 = scrabPostRepository.save(scrab2);
        scrab3 = scrabPostRepository.save(scrab3);

        PageRequest pageRequest = PageRequest.of(0, 10);

        // When - 커서 없이 첫 페이지
        List<BookmarkDto> firstPage = scrabPostRepository.findBookmarksWithCursor(testUser, null, pageRequest);

        // Then
        assertThat(firstPage).hasSize(3);
        assertThat(firstPage.get(0).postId()).isEqualTo(testPost3.getId());
        assertThat(firstPage.get(1).postId()).isEqualTo(testPost2.getId());
        assertThat(firstPage.get(2).postId()).isEqualTo(testPost1.getId());

        // When - 커서를 사용한 다음 페이지
        Long lastBookmarkId = scrab3.getId();
        List<BookmarkDto> nextPage = scrabPostRepository.findBookmarksWithCursor(testUser, lastBookmarkId, pageRequest);

        // Then
        assertThat(nextPage).hasSize(2);
        assertThat(nextPage.get(0).postId()).isEqualTo(testPost2.getId());
        assertThat(nextPage.get(1).postId()).isEqualTo(testPost1.getId());
    }
}
