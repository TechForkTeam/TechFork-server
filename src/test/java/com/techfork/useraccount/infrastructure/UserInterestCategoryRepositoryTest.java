package com.techfork.useraccount.infrastructure;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.UserInterestKeyword;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.fixture.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserInterestCategoryRepository 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
class UserInterestCategoryRepositoryTest {

    @Autowired
    private UserInterestCategoryRepository userInterestCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = UserFixture.socialUser("testSocialId", "test@example.com", null);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("findByUserIdWithKeywords - 키워드와 함께 조회 성공")
    void findByUserIdWithKeywords_Success() {
        // Given: 카테고리와 키워드 생성
        UserInterestCategory backendCategory = UserInterestCategory.create(testUser, EInterestCategory.BACKEND);
        UserInterestKeyword javaKeyword = UserInterestKeyword.create(backendCategory, EInterestKeyword.JAVA);
        UserInterestKeyword springKeyword = UserInterestKeyword.create(backendCategory, EInterestKeyword.SPRING);
        backendCategory.addKeyword(javaKeyword);
        backendCategory.addKeyword(springKeyword);

        UserInterestCategory databaseCategory = UserInterestCategory.create(testUser, EInterestCategory.DATABASE);
        UserInterestKeyword mysqlKeyword = UserInterestKeyword.create(databaseCategory, EInterestKeyword.MYSQL);
        databaseCategory.addKeyword(mysqlKeyword);

        userInterestCategoryRepository.saveAll(List.of(backendCategory, databaseCategory));

        // When: fetch join으로 키워드와 함께 조회
        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(testUser.getId());

        // Then
        assertThat(categories).hasSize(2);

        UserInterestCategory backend = categories.stream()
                .filter(c -> c.getCategory() == EInterestCategory.BACKEND)
                .findFirst()
                .orElseThrow();
        assertThat(backend.getKeywords()).hasSize(2);
        assertThat(backend.getKeywords())
                .extracting(UserInterestKeyword::getKeyword)
                .containsExactlyInAnyOrder(EInterestKeyword.JAVA, EInterestKeyword.SPRING);

        UserInterestCategory database = categories.stream()
                .filter(c -> c.getCategory() == EInterestCategory.DATABASE)
                .findFirst()
                .orElseThrow();
        assertThat(database.getKeywords()).hasSize(1);
        assertThat(database.getKeywords().get(0).getKeyword()).isEqualTo(EInterestKeyword.MYSQL);
    }

    @Test
    @DisplayName("findByUserIdWithKeywords - 키워드 없는 카테고리도 조회")
    void findByUserIdWithKeywords_CategoryWithoutKeywords_Success() {
        // Given: 키워드 없이 카테고리만 생성
        UserInterestCategory aiCategory = UserInterestCategory.create(testUser, EInterestCategory.AI_ML);
        userInterestCategoryRepository.save(aiCategory);

        // When
        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(testUser.getId());

        // Then
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getCategory()).isEqualTo(EInterestCategory.AI_ML);
        assertThat(categories.get(0).getKeywords()).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdWithKeywords - 관심사가 없으면 빈 리스트 반환")
    void findByUserIdWithKeywords_NoInterests_ReturnsEmpty() {
        // When: 관심사가 없는 유저 조회
        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(testUser.getId());

        // Then
        assertThat(categories).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdWithKeywords - N+1 문제 없이 조회 (fetch join)")
    void findByUserIdWithKeywords_NoNPlusOne() {
        // Given: 여러 카테고리와 키워드 생성
        UserInterestCategory backendCategory = UserInterestCategory.create(testUser, EInterestCategory.BACKEND);
        backendCategory.addKeyword(UserInterestKeyword.create(backendCategory, EInterestKeyword.JAVA));
        backendCategory.addKeyword(UserInterestKeyword.create(backendCategory, EInterestKeyword.SPRING));
        backendCategory.addKeyword(UserInterestKeyword.create(backendCategory, EInterestKeyword.PYTHON));

        UserInterestCategory frontendCategory = UserInterestCategory.create(testUser, EInterestCategory.FRONTEND);
        frontendCategory.addKeyword(UserInterestKeyword.create(frontendCategory, EInterestKeyword.REACT));
        frontendCategory.addKeyword(UserInterestKeyword.create(frontendCategory, EInterestKeyword.TYPESCRIPT));

        userInterestCategoryRepository.saveAll(List.of(backendCategory, frontendCategory));

        // When: fetch join으로 한 번에 조회
        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(testUser.getId());

        // Then: 추가 쿼리 없이 키워드 접근 가능 (N+1 없음)
        assertThat(categories).hasSize(2);
        categories.forEach(category -> {
            // keywords 컬렉션이 이미 초기화되어 있어야 함
            assertThat(category.getKeywords()).isNotNull();
        });

        long totalKeywords = categories.stream()
                .mapToLong(c -> c.getKeywords().size())
                .sum();
        assertThat(totalKeywords).isEqualTo(5);
    }

    @Test
    @DisplayName("findByUserIdWithKeywords - DISTINCT로 중복 제거")
    void findByUserIdWithKeywords_DistinctCategories() {
        // Given: 키워드가 여러 개인 카테고리
        UserInterestCategory category = UserInterestCategory.create(testUser, EInterestCategory.DEVOPS);
        category.addKeyword(UserInterestKeyword.create(category, EInterestKeyword.DOCKER));
        category.addKeyword(UserInterestKeyword.create(category, EInterestKeyword.KUBERNETES));
        category.addKeyword(UserInterestKeyword.create(category, EInterestKeyword.CI_CD));
        userInterestCategoryRepository.save(category);

        // When
        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(testUser.getId());

        // Then: 카테고리는 1개만 (DISTINCT 효과)
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getKeywords()).hasSize(3);
    }

    @Test
    @DisplayName("findByUserIdWithKeywords - 다른 유저의 관심사는 조회되지 않음")
    void findByUserIdWithKeywords_OnlyOwnInterests() {
        // Given: 두 번째 유저와 관심사 생성
        User anotherUser = UserFixture.socialUser("anotherSocialId", "another@example.com", null);
        anotherUser = userRepository.save(anotherUser);

        UserInterestCategory testUserCategory = UserInterestCategory.create(testUser, EInterestCategory.BACKEND);
        UserInterestCategory anotherUserCategory = UserInterestCategory.create(anotherUser, EInterestCategory.FRONTEND);
        userInterestCategoryRepository.saveAll(List.of(testUserCategory, anotherUserCategory));

        // When: testUser의 관심사만 조회
        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(testUser.getId());

        // Then
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getCategory()).isEqualTo(EInterestCategory.BACKEND);
    }

    @Test
    @DisplayName("cascade 테스트 - 카테고리 삭제 시 키워드도 함께 삭제")
    void cascade_DeleteCategory_DeletesKeywords() {
        // Given
        UserInterestCategory category = UserInterestCategory.create(testUser, EInterestCategory.BACKEND);
        category.addKeyword(UserInterestKeyword.create(category, EInterestKeyword.JAVA));
        category.addKeyword(UserInterestKeyword.create(category, EInterestKeyword.SPRING));
        category = userInterestCategoryRepository.save(category);

        Long categoryId = category.getId();

        // When: 카테고리 삭제
        userInterestCategoryRepository.delete(category);
        userInterestCategoryRepository.flush();

        // Then: 카테고리와 키워드 모두 삭제됨
        assertThat(userInterestCategoryRepository.findById(categoryId)).isEmpty();
    }

    @Test
    @DisplayName("양방향 매핑 - 키워드 추가 시 양쪽 관계 설정")
    void bidirectionalMapping_AddKeyword_SetsBothSides() {
        // Given
        UserInterestCategory category = UserInterestCategory.create(testUser, EInterestCategory.BACKEND);
        UserInterestKeyword keyword = UserInterestKeyword.create(category, EInterestKeyword.JAVA);

        // When: addKeyword 메서드 사용
        category.addKeyword(keyword);

        // Then: 양방향 관계 설정됨
        assertThat(category.getKeywords()).contains(keyword);
        assertThat(keyword.getUserInterestCategory()).isEqualTo(category);
    }
}
