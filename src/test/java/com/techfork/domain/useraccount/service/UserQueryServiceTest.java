package com.techfork.domain.useraccount.service;

import com.techfork.domain.useraccount.converter.UserConverter;
import com.techfork.domain.useraccount.dto.AccountProfileResponse;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private UserQueryService userQueryService;

    private User testUser;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        testUser = User.createSocialUser(SocialType.KAKAO, "socialId123", "test@example.com", "profile.jpg");
        testUser.updateUser("테스트유저", "test@example.com", "백엔드 개발자입니다.");
        ReflectionTestUtils.setField(testUser, "id", userId);
    }

    @Test
    @DisplayName("계정 프로필 조회 성공")
    void getAccountProfile_Success() {
        // Given
        AccountProfileResponse expectedResponse = AccountProfileResponse.builder()
                .profileImage("profile.jpg")
                .nickName("테스트유저")
                .email("test@example.com")
                .description("백엔드 개발자입니다.")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(userConverter.toAccountProfileResponse(testUser)).willReturn(expectedResponse);

        // When
        AccountProfileResponse result = userQueryService.getAccountProfile(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.profileImage()).isEqualTo("profile.jpg");
        assertThat(result.nickName()).isEqualTo("테스트유저");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.description()).isEqualTo("백엔드 개발자입니다.");

        verify(userRepository).findById(userId);
        verify(userConverter).toAccountProfileResponse(testUser);
    }

    @Test
    @DisplayName("계정 프로필 조회 실패 - 사용자를 찾을 수 없음")
    void getAccountProfile_Fail_UserNotFound() {
        // Given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userQueryService.getAccountProfile(userId))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
    }
}
