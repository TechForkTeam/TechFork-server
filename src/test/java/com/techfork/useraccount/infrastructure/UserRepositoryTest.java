package com.techfork.useraccount.infrastructure;

import com.techfork.activity.readpost.domain.ReadPost;
import com.techfork.activity.bookmark.domain.Bookmark;
import com.techfork.activity.readhistory.domain.SearchHistory;
import com.techfork.activity.bookmark.fixture.BookmarkFixture;
import com.techfork.activity.readhistory.fixture.SearchHistoryFixture;
import com.techfork.activity.readpost.fixture.ReadPostFixture;
import com.techfork.activity.readpost.infrastructure.ReadPostRepository;
import com.techfork.activity.bookmark.infrastructure.BookmarkRepository;
import com.techfork.activity.readhistory.infrastructure.SearchHistoryRepository;
import com.techfork.post.domain.Post;
import com.techfork.post.fixture.PostFixture;
import com.techfork.post.infrastructure.PostRepository;
import com.techfork.domain.source.entity.TechBlog;
import com.techfork.domain.source.fixture.TechBlogFixture;
import com.techfork.domain.source.repository.TechBlogRepository;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.vo.UserInterestSelection;
import com.techfork.useraccount.fixture.UserFixture;
import com.techfork.useraccount.fixture.UserInterestCategoryFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
        testUser = UserFixture.socialUser("testSocialId", "test@example.com", null);
        testUser = userRepository.save(testUser);

        testTechBlog = TechBlogFixture.createTechBlog("테스트 회사", "https://test.com", "https://test.com/rss", null);
        testTechBlog = techBlogRepository.save(testTechBlog);
    }

    @Nested
    @DisplayName("findByIdWithInterestCategories")
    class FindByIdWithInterestCategories {

        @Test
        @DisplayName("findByIdWithInterestCategories - 관심사 카테고리와 함께 조회")
        void userWithInterests_ReturnsUserWithCategories() {
            // Given: 관심사 카테고리 추가
            UserInterestCategory category1 = UserInterestCategoryFixture.interestCategory(testUser, EInterestCategory.BACKEND);
            UserInterestCategory category2 = UserInterestCategoryFixture.interestCategory(testUser, EInterestCategory.DATABASE);
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
        void userWithoutInterests_ReturnsUser() {
            // When: 관심사 없는 유저 조회
            Optional<User> result = userRepository.findByIdWithInterestCategories(testUser.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getInterestCategories()).isEmpty();
        }

        @Test
        @DisplayName("findByIdWithInterestCategories - 존재하지 않는 유저는 Empty 반환")
        void userNotFound_ReturnsEmpty() {
            // When
            Optional<User> result = userRepository.findByIdWithInterestCategories(99999L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("replaceInterests")
    class ReplaceInterests {

        @Test
        @DisplayName("replaceInterests - flush 후 기존 관심사 row를 제거하고 새 관심사 graph를 저장")
        void newInterestGraph_FlushesOrphansAndPersists() {
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
    }

    @Nested
    @DisplayName("findActiveUsersSince")
    class FindActiveUsersSince {

        @Test
        @DisplayName("findActiveUsersSince - 최근 읽은 포스트가 있는 유저 조회")
        void recentReadPost_ReturnsActiveUsers() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusDays(7);

            Post post = createPost();
            ReadPost readPost = ReadPostFixture.createReadPost(testUser, post, LocalDateTime.now(), 100);
            readPostRepository.save(readPost);

            // When: 최근 7일 이내 활동한 유저 조회
            List<User> activeUsers = userRepository.findActiveUsersSince(since);

            // Then
            assertThat(activeUsers).hasSize(1);
            assertThat(activeUsers.get(0).getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("findActiveUsersSince - 최근 북마크한 포스트가 있는 유저 조회")
        void recentBookmark_ReturnsActiveUsers() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusDays(7);

            Post post = createPost();
            Bookmark bookmark = BookmarkFixture.createBookmark(testUser, post, LocalDateTime.now());
            bookmarkRepository.save(bookmark);

            // When
            List<User> activeUsers = userRepository.findActiveUsersSince(since);

            // Then
            assertThat(activeUsers).hasSize(1);
            assertThat(activeUsers.get(0).getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("findActiveUsersSince - 최근 검색 기록이 있는 유저 조회")
        void recentSearchHistory_ReturnsActiveUsers() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusDays(7);

            SearchHistory searchHistory = SearchHistoryFixture.createSearchHistory(testUser, "테스트 검색", LocalDateTime.now());
            searchHistoryRepository.save(searchHistory);

            // When
            List<User> activeUsers = userRepository.findActiveUsersSince(since);

            // Then
            assertThat(activeUsers).hasSize(1);
            assertThat(activeUsers.get(0).getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("findActiveUsersSince - 오래된 활동만 있으면 조회되지 않음")
        void oldActivity_ReturnsEmpty() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            LocalDateTime oldDate = LocalDateTime.now().minusDays(30); // 30일 전

            Post post = createPost();
            ReadPost readPost = ReadPostFixture.createReadPost(testUser, post, oldDate, 100);
            readPostRepository.save(readPost);

            // When: 최근 7일 이내 활동한 유저만 조회
            List<User> activeUsers = userRepository.findActiveUsersSince(since);

            // Then
            assertThat(activeUsers).isEmpty();
        }

        @Test
        @DisplayName("findActiveUsersSince - 여러 활동이 있으면 DISTINCT로 중복 제거")
        void multipleActivities_ReturnsDistinctUsers() {
            // Given: 같은 유저가 여러 활동
            LocalDateTime since = LocalDateTime.now().minusDays(7);

            Post post1 = createPost();
            Post post2 = createPost();

            ReadPost readPost = ReadPostFixture.createReadPost(testUser, post1, LocalDateTime.now(), 100);
            Bookmark bookmark = BookmarkFixture.createBookmark(testUser, post2, LocalDateTime.now());
            SearchHistory searchHistory = SearchHistoryFixture.createSearchHistory(testUser, "검색", LocalDateTime.now());

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
        void withdrawnUserWithActivity_IsExcluded() {
            // Given: 활성 유저와 탈퇴 유저 생성
            User activeUser = UserFixture.activeUser("activeSocialId", "active@example.com");
            activeUser = userRepository.save(activeUser);

            User withdrawnUser = UserFixture.activeUser("withdrawnSocialId", "withdrawn@example.com");
            withdrawnUser.withdraw(); // 탈퇴 처리
            withdrawnUser = userRepository.save(withdrawnUser);

            LocalDateTime since = LocalDateTime.now().minusDays(7);
            Post post = createPost();

            // 두 유저 모두 최근 활동이 있음
            ReadPost activeUserRead = ReadPostFixture.createReadPost(activeUser, post, LocalDateTime.now(), 100);
            ReadPost withdrawnUserRead = ReadPostFixture.createReadPost(withdrawnUser, post, LocalDateTime.now(), 100);
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
        void onlyWithdrawnUsers_ReturnsEmpty() {
            // Given: 탈퇴한 유저만 생성
            testUser.withdraw();
            userRepository.save(testUser);

            LocalDateTime since = LocalDateTime.now().minusDays(7);
            Post post = createPost();

            // 탈퇴한 유저에게 최근 활동 기록 추가
            ReadPost readPost = ReadPostFixture.createReadPost(testUser, post, LocalDateTime.now(), 100);
            readPostRepository.save(readPost);

            // When
            List<User> activeUsers = userRepository.findActiveUsersSince(since);

            // Then: 탈퇴 회원은 제외되므로 빈 리스트
            assertThat(activeUsers).isEmpty();
        }

        @Test
        @DisplayName("findActiveUsersSince - 여러 유저 중 일부만 탈퇴한 경우")
        void mixedWithdrawnAndActiveUsers_ReturnsOnlyActiveUsers() {
            // Given: 3명의 유저 (1명 탈퇴, 2명 활성)
            User user1 = UserFixture.activeUser("user1", "user1@example.com");
            user1 = userRepository.save(user1);

            User user2 = UserFixture.activeUser("user2", "user2@example.com");
            user2 = userRepository.save(user2);

            User user3Withdrawn = UserFixture.activeUser("user3", "user3@example.com");
            user3Withdrawn.withdraw(); // 탈퇴
            user3Withdrawn = userRepository.save(user3Withdrawn);

            LocalDateTime since = LocalDateTime.now().minusDays(7);
            Post post = createPost();

            // 3명 모두 최근 활동 있음
            readPostRepository.saveAll(List.of(
                    ReadPostFixture.createReadPost(user1, post, LocalDateTime.now(), 100),
                    ReadPostFixture.createReadPost(user2, post, LocalDateTime.now(), 100),
                    ReadPostFixture.createReadPost(user3Withdrawn, post, LocalDateTime.now(), 100)
            ));

            // When
            List<User> activeUsers = userRepository.findActiveUsersSince(since);

            // Then: 활성 회원 2명만 조회
            assertThat(activeUsers).hasSize(2);
            assertThat(activeUsers).extracting(User::getId)
                    .containsExactlyInAnyOrder(user1.getId(), user2.getId())
                    .doesNotContain(user3Withdrawn.getId());
        }
    }

    @Nested
    @DisplayName("findAllWithInterestCategoriesByIds")
    class FindAllWithInterestCategoriesByIds {

        @Test
        @DisplayName("findAllWithInterestCategoriesByIds - 여러 유저를 관심사와 함께 조회")
        void validIds_ReturnsUsersWithInterestCategories() {
            // Given: 두 번째 유저 생성
            User user2 = UserFixture.socialUser("testSocialId2", "test2@example.com", null);
            user2 = userRepository.save(user2);

            UserInterestCategory category1 = UserInterestCategoryFixture.interestCategory(testUser, EInterestCategory.BACKEND);
            UserInterestCategory category2 = UserInterestCategoryFixture.interestCategory(user2, EInterestCategory.FRONTEND);
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
        void invalidIds_FiltersOutMissingUsers() {
            // When
            List<User> users = userRepository.findAllWithInterestCategoriesByIds(
                    List.of(testUser.getId(), 99999L)
            );

            // Then: 존재하는 유저만 조회
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getId()).isEqualTo(testUser.getId());
        }
    }

    private Post createPost() {
        return postRepository.save(PostFixture.createPost(
                testTechBlog,
                "테스트 포스트",
                "내용",
                "내용",
                "테스트 포스트 요약",
                "테스트 포스트 짧은 요약",
                "https://test.com/thumbnail.png",
                "https://test.com/post/" + UUID.randomUUID(),
                LocalDateTime.now()
        ));
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
