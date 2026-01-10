package com.techfork.domain.user.repository;

import com.techfork.domain.activity.entity.ReadPost;
import com.techfork.domain.activity.entity.ScrabPost;
import com.techfork.domain.activity.entity.SearchHistory;
import com.techfork.domain.activity.repository.ReadPostRepository;
import com.techfork.domain.activity.repository.ScrabPostRepository;
import com.techfork.domain.activity.repository.SearchHistoryRepository;
import com.techfork.domain.post.entity.Post;
import com.techfork.domain.post.repository.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.entity.UserInterestCategory;
import com.techfork.domain.user.enums.EInterestCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReadPostRepository readPostRepository;

    @Autowired
    private ScrabPostRepository scrabPostRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TechBlogRepository techBlogRepository;

    private User testUser;
    private TechBlog testTechBlog;

    @BeforeEach
    void setUp() {
        testUser = User.create();
        testUser = userRepository.save(testUser);

        testTechBlog = TechBlog.builder()
                .companyName("테스트 회사")
                .blogUrl("https://test.com")
                .rssUrl("https://test.com/rss")
                .build();
        testTechBlog = techBlogRepository.save(testTechBlog);
    }

    @Test
    @DisplayName("findByIdWithInterestCategories - 관심사 카테고리와 함께 조회")
    void findByIdWithInterestCategories_Success() {
        // Given: 관심사 카테고리 추가
        UserInterestCategory category1 = UserInterestCategory.create(testUser, EInterestCategory.BACKEND);
        UserInterestCategory category2 = UserInterestCategory.create(testUser, EInterestCategory.DATABASE);
        testUser.getInterestCategories().add(category1);
        testUser.getInterestCategories().add(category2);
        userRepository.save(testUser);

        // When: fetch join으로 조회
        Optional<User> result = userRepository.findByIdWithInterestCategories(testUser.getId());

        // Then
        assertThat(result).isPresent();
        User user = result.get();
        assertThat(user.getInterestCategories()).hasSize(2);
        assertThat(user.getInterestCategories())
                .extracting(UserInterestCategory::getCategory)
                .containsExactlyInAnyOrder(EInterestCategory.BACKEND, EInterestCategory.DATABASE);
    }

    @Test
    @DisplayName("findByIdWithInterestCategories - 관심사가 없어도 조회 성공")
    void findByIdWithInterestCategories_NoInterests_Success() {
        // When: 관심사 없는 유저 조회
        Optional<User> result = userRepository.findByIdWithInterestCategories(testUser.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getInterestCategories()).isEmpty();
    }

    @Test
    @DisplayName("findByIdWithInterestCategories - 존재하지 않는 유저는 Empty 반환")
    void findByIdWithInterestCategories_NotFound_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByIdWithInterestCategories(99999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findActiveUsersSince - 최근 읽은 포스트가 있는 유저 조회")
    void findActiveUsersSince_WithReadPost_ReturnsActiveUsers() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        Post post = createPost();
        ReadPost readPost = ReadPost.create(testUser, post, LocalDateTime.now(), 100);
        readPostRepository.save(readPost);

        // When: 최근 7일 이내 활동한 유저 조회
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findActiveUsersSince - 최근 스크랩한 포스트가 있는 유저 조회")
    void findActiveUsersSince_WithScrapPost_ReturnsActiveUsers() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        Post post = createPost();
        ScrabPost scrabPost = ScrabPost.create(testUser, post, LocalDateTime.now());
        scrabPostRepository.save(scrabPost);

        // When
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findActiveUsersSince - 최근 검색 기록이 있는 유저 조회")
    void findActiveUsersSince_WithSearchHistory_ReturnsActiveUsers() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        SearchHistory searchHistory = SearchHistory.create(testUser, "테스트 검색", LocalDateTime.now());
        searchHistoryRepository.save(searchHistory);

        // When
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findActiveUsersSince - 오래된 활동만 있으면 조회되지 않음")
    void findActiveUsersSince_OldActivity_ReturnsEmpty() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        LocalDateTime oldDate = LocalDateTime.now().minusDays(30); // 30일 전

        Post post = createPost();
        ReadPost readPost = ReadPost.create(testUser, post, oldDate, 100);
        readPostRepository.save(readPost);

        // When: 최근 7일 이내 활동한 유저만 조회
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then
        assertThat(activeUsers).isEmpty();
    }

    @Test
    @DisplayName("findActiveUsersSince - 여러 활동이 있으면 DISTINCT로 중복 제거")
    void findActiveUsersSince_MultipleActivities_ReturnsDistinct() {
        // Given: 같은 유저가 여러 활동
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        Post post1 = createPost();
        Post post2 = createPost();

        ReadPost readPost = ReadPost.create(testUser, post1, LocalDateTime.now(), 100);
        ScrabPost scrabPost = ScrabPost.create(testUser, post2, LocalDateTime.now());
        SearchHistory searchHistory = SearchHistory.create(testUser, "검색", LocalDateTime.now());

        readPostRepository.save(readPost);
        scrabPostRepository.save(scrabPost);
        searchHistoryRepository.save(searchHistory);

        // When
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then: 중복 없이 1명만 조회
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findAllWithInterestCategoriesByIds - 여러 유저를 관심사와 함께 조회")
    void findAllWithInterestCategoriesByIds_Success() {
        // Given: 두 번째 유저 생성
        User user2 = User.create();
        user2 = userRepository.save(user2);

        UserInterestCategory category1 = UserInterestCategory.create(testUser, EInterestCategory.BACKEND);
        UserInterestCategory category2 = UserInterestCategory.create(user2, EInterestCategory.FRONTEND);
        testUser.getInterestCategories().add(category1);
        user2.getInterestCategories().add(category2);
        userRepository.saveAll(List.of(testUser, user2));

        // When
        List<User> users = userRepository.findAllWithInterestCategoriesByIds(
                List.of(testUser.getId(), user2.getId())
        );

        // Then
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getInterestCategories()).isNotEmpty();
        assertThat(users.get(1).getInterestCategories()).isNotEmpty();
    }

    @Test
    @DisplayName("findAllWithInterestCategoriesByIds - 존재하지 않는 ID는 제외")
    void findAllWithInterestCategoriesByIds_InvalidIds_FiltersOut() {
        // When
        List<User> users = userRepository.findAllWithInterestCategoriesByIds(
                List.of(testUser.getId(), 99999L)
        );

        // Then: 존재하는 유저만 조회
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo(testUser.getId());
    }

    private Post createPost() {
        return postRepository.save(Post.builder()
                .title("테스트 포스트")
                .fullContent("내용")
                .plainContent("내용")
                .company("테스트 회사")
                .url("https://test.com/post/" + System.currentTimeMillis())
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog)
                .build());
    }
}
