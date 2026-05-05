package com.techfork.domain.activity.readhistory.repository;

import com.techfork.domain.activity.readhistory.entity.SearchHistory;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class SearchHistoryRepositoryTest {

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", "profile.jpg");
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        searchHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("최근 검색 히스토리 조회")
    class FindRecentSearchHistoriesByUserId {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("최근 순으로 정렬된다")
            void findRecentSearchHistoriesByUserId_Success() {
                SearchHistory history1 = SearchHistory.create(testUser, "Spring Boot", LocalDateTime.now().minusHours(3));
                SearchHistory history2 = SearchHistory.create(testUser, "Java", LocalDateTime.now().minusHours(2));
                SearchHistory history3 = SearchHistory.create(testUser, "Kotlin", LocalDateTime.now().minusHours(1));
                searchHistoryRepository.saveAll(List.of(history1, history2, history3));

                PageRequest pageRequest = PageRequest.of(0, 10);

                List<SearchHistory> result = searchHistoryRepository.findRecentSearchHistoriesByUserId(testUser.getId(), pageRequest);

                assertThat(result).hasSize(3);
                assertThat(result.get(0).getQuery()).isEqualTo("Kotlin");
                assertThat(result.get(1).getQuery()).isEqualTo("Java");
                assertThat(result.get(2).getQuery()).isEqualTo("Spring Boot");
            }
        }
    }
}
