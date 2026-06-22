package com.techfork.useraccount.application.query.lookup;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.fixture.UserFixture;
import com.techfork.useraccount.fixture.UserInterestCategoryFixture;
import com.techfork.useraccount.infrastructure.UserInterestCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserInterestLookupServiceTest {

    @Mock
    private UserInterestCategoryRepository userInterestCategoryRepository;

    @InjectMocks
    private UserInterestLookupService userInterestLookupService;

    @Test
    @DisplayName("사용자의 관심 키워드 display name 목록을 반환한다")
    void getInterestKeywordDisplayNames_ReturnsKeywordDisplayNames() {
        Long userId = 1L;
        User user = UserFixture.socialUser("social-1", "user@example.com", null);
        UserInterestCategory backend = UserInterestCategoryFixture.interestCategory(user, EInterestCategory.BACKEND, EInterestKeyword.JAVA, EInterestKeyword.SPRING);
        UserInterestCategory devops = UserInterestCategoryFixture.interestCategory(user, EInterestCategory.DEVOPS, EInterestKeyword.DOCKER);
        given(userInterestCategoryRepository.findByUserIdWithKeywords(userId))
                .willReturn(List.of(backend, devops));

        List<String> result = userInterestLookupService.getInterestKeywordDisplayNames(userId);

        assertThat(result).containsExactly("Java", "Spring", "Docker");
        verify(userInterestCategoryRepository).findByUserIdWithKeywords(userId);
    }

    @Test
    @DisplayName("관심사가 없으면 빈 목록을 반환한다")
    void getInterestKeywordDisplayNames_NoInterests_ReturnsEmptyList() {
        Long userId = 2L;
        given(userInterestCategoryRepository.findByUserIdWithKeywords(userId)).willReturn(List.of());

        List<String> result = userInterestLookupService.getInterestKeywordDisplayNames(userId);

        assertThat(result).isEmpty();
        verify(userInterestCategoryRepository).findByUserIdWithKeywords(userId);
    }

}
