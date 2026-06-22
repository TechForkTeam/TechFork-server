package com.techfork.useraccount.application.query;

import com.techfork.useraccount.application.query.result.GetAccountProfileResult;
import com.techfork.useraccount.application.reader.UserAggregateReader;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.fixture.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserAggregateReader userAggregateReader;

    @InjectMocks
    private UserQueryService userQueryService;

    private User testUser;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        testUser = UserFixture.activeUserWithId(userId, "socialId123", "test@example.com", "profile.jpg");
    }

    @Nested
    @DisplayName("계정 프로필 조회")
    class GetAccountProfile {

        @Test
        @DisplayName("계정 프로필 조회 성공")
        void existingUser_ReturnsAccountProfile() {
            given(userAggregateReader.getById(userId)).willReturn(testUser);

            GetAccountProfileResult result = userQueryService.getAccountProfile(new GetAccountProfileQuery(userId));

            assertThat(result).isNotNull();
            assertThat(result.profileImage()).isEqualTo("profile.jpg");
            assertThat(result.nickName()).isEqualTo("테스트유저");
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.description()).isEqualTo("백엔드 개발자입니다.");
            verify(userAggregateReader).getById(userId);
        }

        @Test
        @DisplayName("계정 프로필 조회 실패 - 사용자를 찾을 수 없음")
        void userNotFound_ThrowsUserNotFound() {
            given(userAggregateReader.getById(userId)).willThrow(new GeneralException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userQueryService.getAccountProfile(new GetAccountProfileQuery(userId)))
                    .isInstanceOf(GeneralException.class)
                    .extracting(ex -> ((GeneralException) ex).getCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(userAggregateReader).getById(userId);
        }
    }

}
