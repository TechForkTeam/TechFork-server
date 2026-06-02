package com.techfork.useraccount.infrastructure;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.readhistory.domain.SearchHistory;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.activity.readhistory.infrastructure.SearchHistoryRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.vo.UserInterestSelection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private UserInterestCategoryRepository userInterestCategoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ReadPostRepository readPostRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

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
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
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
    @DisplayName("replaceInterests - flush 후 기존 관심사 row를 제거하고 새 관심사 graph를 저장")
    void replaceInterests_FlushesOrphansAndPersistsNewGraph() {
        // Given: 기존 관심사 graph 저장
        testUser.replaceInterests(List.of(
                new UserInterestSelection(EInterestCategory.BACKEND, List.of(EInterestKeyword.JAVA, EInterestKeyword.SPRING)),
                new UserInterestSelection(EInterestCategory.DATABASE, List.of(EInterestKeyword.MYSQL))
        ));
        userRepository.saveAndFlush(testUser);
        entityManager.clear();

        List<Long> oldCategoryIds = userInterestCategoryRepository.findByUserIdWithKeywords(testUser.getId()).stream()
                .map(UserInterestCategory::getId)
                .toList();
        List<Long> oldKeywordIds = findKeywordIdsByCategoryIds(oldCategoryIds);

        User user = userRepository.findByIdWithInterestCategories(testUser.getId()).orElseThrow();

        // When: 관심사를 새 graph로 완전히 교체하고 flush/clear
        user.replaceInterests(List.of(
                new UserInterestSelection(EInterestCategory.FRONTEND, List.of(EInterestKeyword.REACT))
        ));
        userRepository.flush();
        entityManager.clear();

        // Then: 기존 row는 orphanRemoval로 제거되고 새 graph만 남는다
        assertThat(countCategoriesByIds(oldCategoryIds)).isZero();
        assertThat(countKeywordsByIds(oldKeywordIds)).isZero();

        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(testUser.getId());
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getCategory()).isEqualTo(EInterestCategory.FRONTEND);
        assertThat(categories.get(0).getKeywords())
                .extracting(keyword -> keyword.getKeyword())
                .containsExactly(EInterestKeyword.REACT);
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
    @DisplayName("findActiveUsersSince - 최근 북마크한 포스트가 있는 유저 조회")
    void findActiveUsersSince_WithBookmark_ReturnsActiveUsers() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        Post post = createPost();
        Bookmark bookmark = Bookmark.create(testUser, post, LocalDateTime.now());
        bookmarkRepository.save(bookmark);

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
        Bookmark bookmark = Bookmark.create(testUser, post2, LocalDateTime.now());
        SearchHistory searchHistory = SearchHistory.create(testUser, "검색", LocalDateTime.now());

        readPostRepository.save(readPost);
        bookmarkRepository.save(bookmark);
        searchHistoryRepository.save(searchHistory);

        // When
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then: 중복 없이 1명만 조회
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findActiveUsersSince - 탈퇴한 회원은 제외")
    void findActiveUsersSince_ExcludesWithdrawnUsers() {
        // Given: 활성 유저와 탈퇴 유저 생성
        User activeUser = User.createSocialUser(SocialType.KAKAO, "activeSocialId", "active@example.com", null);
        activeUser = userRepository.save(activeUser);

        User withdrawnUser = User.createSocialUser(SocialType.KAKAO, "withdrawnSocialId", "withdrawn@example.com", null);
        withdrawnUser.updateUser("탈퇴유저", "withdrawn@example.com", "개발자였습니다.");
        withdrawnUser.withdraw(); // 탈퇴 처리
        withdrawnUser = userRepository.save(withdrawnUser);

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Post post = createPost();

        // 두 유저 모두 최근 활동이 있음
        ReadPost activeUserRead = ReadPost.create(activeUser, post, LocalDateTime.now(), 100);
        ReadPost withdrawnUserRead = ReadPost.create(withdrawnUser, post, LocalDateTime.now(), 100);
        readPostRepository.saveAll(List.of(activeUserRead, withdrawnUserRead));

        // When: 최근 활동한 유저 조회
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then: 탈퇴한 유저는 제외되고 활성 유저만 조회
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getId()).isEqualTo(activeUser.getId());
        assertThat(activeUsers).extracting(User::getId).doesNotContain(withdrawnUser.getId());
    }

    @Test
    @DisplayName("findActiveUsersSince - 탈퇴 회원만 있으면 빈 리스트 반환")
    void findActiveUsersSince_OnlyWithdrawnUsers_ReturnsEmpty() {
        // Given: 탈퇴한 유저만 생성
        testUser.withdraw();
        userRepository.save(testUser);

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Post post = createPost();

        // 탈퇴한 유저에게 최근 활동 기록 추가
        ReadPost readPost = ReadPost.create(testUser, post, LocalDateTime.now(), 100);
        readPostRepository.save(readPost);

        // When
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then: 탈퇴 회원은 제외되므로 빈 리스트
        assertThat(activeUsers).isEmpty();
    }

    @Test
    @DisplayName("findActiveUsersSince - 여러 유저 중 일부만 탈퇴한 경우")
    void findActiveUsersSince_MixedWithdrawnAndActiveUsers() {
        // Given: 3명의 유저 (1명 탈퇴, 2명 활성)
        User user1 = User.createSocialUser(SocialType.KAKAO, "user1", "user1@example.com", null);
        user1 = userRepository.save(user1);

        User user2 = User.createSocialUser(SocialType.KAKAO, "user2", "user2@example.com", null);
        user2 = userRepository.save(user2);

        User user3Withdrawn = User.createSocialUser(SocialType.KAKAO, "user3", "user3@example.com", null);
        user3Withdrawn.withdraw(); // 탈퇴
        user3Withdrawn = userRepository.save(user3Withdrawn);

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Post post = createPost();

        // 3명 모두 최근 활동 있음
        readPostRepository.saveAll(List.of(
                ReadPost.create(user1, post, LocalDateTime.now(), 100),
                ReadPost.create(user2, post, LocalDateTime.now(), 100),
                ReadPost.create(user3Withdrawn, post, LocalDateTime.now(), 100)
        ));

        // When
        List<User> activeUsers = userRepository.findActiveUsersSince(since);

        // Then: 활성 회원 2명만 조회
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).extracting(User::getId)
                .containsExactlyInAnyOrder(user1.getId(), user2.getId())
                .doesNotContain(user3Withdrawn.getId());
    }

    @Test
    @DisplayName("findAllWithInterestCategoriesByIds - 여러 유저를 관심사와 함께 조회")
    void findAllWithInterestCategoriesByIds_Success() {
        // Given: 두 번째 유저 생성
        User user2 = User.createSocialUser(SocialType.KAKAO, "testSocialId2", "test2@example.com", null);
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
                .url("https://test.com/post/" + UUID.randomUUID())
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .techBlog(testTechBlog)
                .build());
    }

    private List<Long> findKeywordIdsByCategoryIds(List<Long> categoryIds) {
        return entityManager.createQuery("""
                        SELECT keyword.id
                        FROM UserInterestKeyword keyword
                        WHERE keyword.userInterestCategory.id IN :categoryIds
                        """, Long.class)
                .setParameter("categoryIds", categoryIds)
                .getResultList();
    }

    private Long countCategoriesByIds(List<Long> categoryIds) {
        return entityManager.createQuery("""
                        SELECT COUNT(category)
                        FROM UserInterestCategory category
                        WHERE category.id IN :categoryIds
                        """, Long.class)
                .setParameter("categoryIds", categoryIds)
                .getSingleResult();
    }

    private Long countKeywordsByIds(List<Long> keywordIds) {
        return entityManager.createQuery("""
                        SELECT COUNT(keyword)
                        FROM UserInterestKeyword keyword
                        WHERE keyword.id IN :keywordIds
                        """, Long.class)
                .setParameter("keywordIds", keywordIds)
                .getSingleResult();
    }
}
