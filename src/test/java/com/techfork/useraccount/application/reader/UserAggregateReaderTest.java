package com.techfork.useraccount.application.reader;

import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAggregateReaderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAggregateReader userAggregateReader;

    @Nested
    @DisplayName("사용자 조회")
    class GetById {

        @Test
        @DisplayName("ID로 사용자를 조회한다")
        void existingUser_ReturnsUser() {
            Long userId = 1L;
            User user = mock(User.class);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            User result = userAggregateReader.getById(userId);

            assertThat(result).isSameAs(user);
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("ID로 사용자를 찾지 못하면 예외를 던진다")
        void userNotFound_ThrowsUserNotFound() {
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userAggregateReader.getById(userId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);
            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("관심사 포함 사용자 조회")
    class GetByIdWithInterestCategories {

        @Test
        @DisplayName("관심사 카테고리와 함께 사용자를 조회한다")
        void existingUser_ReturnsUserWithInterestCategories() {
            Long userId = 1L;
            User user = mock(User.class);
            given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.of(user));

            User result = userAggregateReader.getByIdWithInterestCategories(userId);

            assertThat(result).isSameAs(user);
            verify(userRepository).findByIdWithInterestCategories(userId);
        }

        @Test
        @DisplayName("관심사 카테고리 조회에서 사용자를 찾지 못하면 예외를 던진다")
        void userNotFound_ThrowsUserNotFound() {
            Long userId = 999L;
            given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userAggregateReader.getByIdWithInterestCategories(userId))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);
            verify(userRepository).findByIdWithInterestCategories(userId);
        }
    }

}
