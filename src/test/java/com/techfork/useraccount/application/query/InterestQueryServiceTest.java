package com.techfork.useraccount.application.query;

import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.application.query.result.GetUserInterestsResult;
import com.techfork.useraccount.application.reader.UserReader;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserInterestCategoryRepository;
import org.junit.jupiter.api.DisplayName;
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
    private UserReader userReader;

    @Mock
    private UserInterestCategoryRepository userInterestCategoryRepository;

    @InjectMocks
    private InterestQueryService interestQueryService;

    @Test
    @DisplayName("사용자 관심사 조회 - 사용자 조회 후 관심사를 반환한다")
    void getUserInterests_Success() {
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        given(userReader.getById(userId)).willReturn(user);
        given(userInterestCategoryRepository.findByUserIdWithKeywords(userId)).willReturn(List.of());

        GetUserInterestsResult result = interestQueryService.getUserInterests(new GetUserInterestsQuery(userId));

        assertThat(result.interests()).isEmpty();
        verify(userReader).getById(userId);
        verify(userInterestCategoryRepository).findByUserIdWithKeywords(userId);
    }

    @Test
    @DisplayName("사용자 관심사 조회 - 사용자가 없으면 관심사를 조회하지 않는다")
    void getUserInterests_UserNotFound_ThrowsException() {
        Long userId = 999L;
        given(userReader.getById(userId)).willThrow(new GeneralException(UserErrorCode.USER_NOT_FOUND));

        assertThatThrownBy(() -> interestQueryService.getUserInterests(new GetUserInterestsQuery(userId)))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);
        verify(userReader).getById(userId);
        verify(userInterestCategoryRepository, never()).findByUserIdWithKeywords(userId);
    }
}
