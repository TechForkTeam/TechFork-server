package com.techfork.useraccount.application.query;

import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.application.query.result.GetUserInterestsResult;
import com.techfork.useraccount.application.reader.UserAggregateReader;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserInterestCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterestQueryServiceTest {

    @Mock
    private UserAggregateReader userAggregateReader;

    @Mock
    private UserInterestCategoryRepository userInterestCategoryRepository;

    @InjectMocks
    private InterestQueryService interestQueryService;

    @Nested
    @DisplayName("사용자 관심사 조회")
    class GetUserInterests {

        @Test
        @DisplayName("사용자 관심사 조회 - 사용자 조회 후 관심사를 반환한다")
        void existingUser_ReturnsUserInterests() {
            Long userId = 1L;
            User user = mock(User.class);
            given(user.getId()).willReturn(userId);
            given(userAggregateReader.getById(userId)).willReturn(user);
            given(userInterestCategoryRepository.findByUserIdWithKeywords(userId)).willReturn(List.of());

            GetUserInterestsResult result = interestQueryService.getUserInterests(new GetUserInterestsQuery(userId));

            assertThat(result.interests()).isEmpty();
            verify(userAggregateReader).getById(userId);
            verify(userInterestCategoryRepository).findByUserIdWithKeywords(userId);
        }

        @Test
        @DisplayName("사용자 관심사 조회 - 사용자가 없으면 관심사를 조회하지 않는다")
        void userNotFound_ThrowsUserNotFound() {
            Long userId = 999L;
            given(userAggregateReader.getById(userId)).willThrow(new GeneralException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> interestQueryService.getUserInterests(new GetUserInterestsQuery(userId)))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);
            verify(userAggregateReader).getById(userId);
            verify(userInterestCategoryRepository, never()).findByUserIdWithKeywords(userId);
        }
    }

}
