package com.techfork.useraccount.application.query.lookup;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupService userLookupService;

    @Nested
    @DisplayName("사용자 조회")
    class GetUserOrThrow {

        @Test
        @DisplayName("존재하는 사용자면 반환한다")
        void existingUser_ReturnsUser() {
            Long userId = 1L;
            User user = mock(User.class);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            User result = userLookupService.getUserOrThrow(userId);

            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 예외를 던진다")
        void userNotFound_ThrowsUserNotFound() {
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userLookupService.getUserOrThrow(userId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("최근 활동 사용자 ID 조회")
    class GetActiveUserIdsSince {

        @Test
        @DisplayName("최근 활동 사용자 ID 목록을 반환한다")
        void sinceDate_ReturnsActiveUserIds() {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            User firstUser = mock(User.class);
            User secondUser = mock(User.class);
            given(firstUser.getId()).willReturn(1L);
            given(secondUser.getId()).willReturn(2L);
            given(userRepository.findActiveUsersSince(since)).willReturn(List.of(firstUser, secondUser));

            List<Long> result = userLookupService.getActiveUserIdsSince(since);

            assertThat(result).containsExactly(1L, 2L);
            verify(userRepository).findActiveUsersSince(since);
        }
    }

}
