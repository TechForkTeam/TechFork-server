package com.techfork.domain.useraccount.service;

import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupService userLookupService;

    @Test
    @DisplayName("존재하는 사용자면 반환한다")
    void getUserOrThrow_Success() {
        Long userId = 1L;
        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        User result = userLookupService.getUserOrThrow(userId);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 예외를 던진다")
    void getUserOrThrow_Fail_UserNotFound() {
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userLookupService.getUserOrThrow(userId))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);
    }
}
