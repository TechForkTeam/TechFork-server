package com.techfork.activity.bookmark.infrastructure;

import com.techfork.activity.bookmark.application.query.BookmarkDto;
import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BookmarkRepositoryTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    @Autowired
    private EntityManager entityManager;

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
        bookmarkRepository.deleteAll();
        postRepository.deleteAll();
        techBlogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("북마크 목록 조회")
    class FindBookmarksWithCursor {

        @Test
        @DisplayName("커서 기반 페이징")
        void findBookmarksWithCursor() {
            Bookmark bookmark1 = Bookmark.create(testUser, testPost1, LocalDateTime.now().minusHours(3));
            Bookmark bookmark2 = Bookmark.create(testUser, testPost2, LocalDateTime.now().minusHours(2));
            Bookmark bookmark3 = Bookmark.create(testUser, testPost3, LocalDateTime.now().minusHours(1));
            bookmark1 = bookmarkRepository.save(bookmark1);
            bookmark2 = bookmarkRepository.save(bookmark2);
            bookmark3 = bookmarkRepository.save(bookmark3);

            PageRequest pageRequest = PageRequest.of(0, 10);

            List<BookmarkDto> firstPage = bookmarkRepository.findBookmarksWithCursor(testUser, null, pageRequest);

            assertThat(firstPage).hasSize(3);
            assertThat(firstPage.get(0).postId()).isEqualTo(testPost3.getId());
            assertThat(firstPage.get(1).postId()).isEqualTo(testPost2.getId());
            assertThat(firstPage.get(2).postId()).isEqualTo(testPost1.getId());

            Long lastBookmarkId = bookmark3.getId();
            List<BookmarkDto> nextPage = bookmarkRepository.findBookmarksWithCursor(testUser, lastBookmarkId, pageRequest);

            assertThat(nextPage).hasSize(2);
            assertThat(nextPage.get(0).postId()).isEqualTo(testPost2.getId());
            assertThat(nextPage.get(1).postId()).isEqualTo(testPost1.getId());
        }
    }

    @Nested
    @DisplayName("북마크된 게시글 ID 목록 조회")
    class FindBookmarkedPostIds {

        @Test
        @DisplayName("북마크된 게시글 ID 목록 조회")
        void findBookmarkedPostIds() {
            Bookmark bookmark1 = Bookmark.create(testUser, testPost1, LocalDateTime.now());
            Bookmark bookmark3 = Bookmark.create(testUser, testPost3, LocalDateTime.now());
            bookmarkRepository.save(bookmark1);
            bookmarkRepository.save(bookmark3);

            List<Long> postIds = List.of(testPost1.getId(), testPost2.getId(), testPost3.getId());

            List<Long> bookmarkedPostIds = bookmarkRepository.findBookmarkedPostIds(testUser.getId(), postIds);

            assertThat(bookmarkedPostIds).hasSize(2);
            assertThat(bookmarkedPostIds).containsExactlyInAnyOrder(testPost1.getId(), testPost3.getId());
            assertThat(bookmarkedPostIds).doesNotContain(testPost2.getId());
        }

        @Test
        @DisplayName("북마크된 게시글이 없을 때 빈 리스트 반환")
        void findBookmarkedPostIds_whenNoBookmarks() {
            List<Long> postIds = List.of(testPost1.getId(), testPost2.getId(), testPost3.getId());

            List<Long> bookmarkedPostIds = bookmarkRepository.findBookmarkedPostIds(testUser.getId(), postIds);

            assertThat(bookmarkedPostIds).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 북마크는 조회되지 않음")
        void findBookmarkedPostIds_differentUser() {
            User anotherUser = User.createSocialUser(SocialType.KAKAO, "anotherSocialId", "another@example.com", "another.jpg");
            anotherUser = userRepository.save(anotherUser);

            Bookmark bookmark1 = Bookmark.create(testUser, testPost1, LocalDateTime.now());
            Bookmark bookmark2 = Bookmark.create(anotherUser, testPost2, LocalDateTime.now());
            bookmarkRepository.save(bookmark1);
            bookmarkRepository.save(bookmark2);

            List<Long> postIds = List.of(testPost1.getId(), testPost2.getId());

            List<Long> bookmarkedPostIds = bookmarkRepository.findBookmarkedPostIds(testUser.getId(), postIds);

            assertThat(bookmarkedPostIds).hasSize(1);
            assertThat(bookmarkedPostIds).containsExactly(testPost1.getId());
        }
    }

    @Nested
    @DisplayName("북마크 저장")
    class Save {

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("같은 사용자와 게시글 조합은 한 번만 북마크할 수 있다")
            void save_duplicateUserAndPostCombination_ThrowsException() {
                Bookmark firstBookmark = Bookmark.create(testUser, testPost1, LocalDateTime.now().minusMinutes(1));
                Bookmark duplicateBookmark = Bookmark.create(testUser, testPost1, LocalDateTime.now());

                bookmarkRepository.saveAndFlush(firstBookmark);

                assertThatThrownBy(() -> bookmarkRepository.saveAndFlush(duplicateBookmark))
                        .isInstanceOf(DataIntegrityViolationException.class);

                entityManager.clear();
            }
        }
    }
}
