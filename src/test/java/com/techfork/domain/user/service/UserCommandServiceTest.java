package com.techfork.domain.user.service;

import com.techfork.domain.user.dto.OnboardingRequest;
import com.techfork.domain.user.dto.SaveInterestRequest;
import com.techfork.domain.user.dto.UserInterestDto;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * UserCommandService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private InterestCommandService interestCommandService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    @DisplayName("온보딩 완료 - 정상 케이스")
    void completeOnboarding_Success() {
        // Given
        Long userId = 1L;
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com");

        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("BACKEND")
                        .keywords(List.of("JAVA", "SPRING"))
                        .build(),
                UserInterestDto.builder()
                        .category("DATABASE")
                        .keywords(List.of("MYSQL", "REDIS"))
                        .build()
        );

        OnboardingRequest request = new OnboardingRequest(
                "테크포크유저",
                "user@techfork.com",
                "백엔드 개발자입니다",
                interests
        );

        given(userRepository.findByIdWithInterestCategories(userId))
                .willReturn(Optional.of(mockUser));

        // When
        userCommandService.completeOnboarding(userId, request);

        // Then
        assertThat(mockUser.getNickName()).isEqualTo("테크포크유저");
        assertThat(mockUser.getEmail()).isEqualTo("user@techfork.com");
        assertThat(mockUser.getDescription()).isEqualTo("백엔드 개발자입니다");

        verify(userRepository, times(1)).findByIdWithInterestCategories(userId);
        verify(interestCommandService, times(1)).saveUserInterests(eq(mockUser), any(SaveInterestRequest.class));
    }

    @Test
    @DisplayName("온보딩 완료 - 사용자가 존재하지 않으면 예외 발생")
    void completeOnboarding_UserNotFound_ThrowsException() {
        // Given
        Long userId = 999L;
        OnboardingRequest request = new OnboardingRequest(
                "테크포크유저",
                "user@techfork.com",
                null,
                List.of(
                        UserInterestDto.builder()
                                .category("BACKEND")
                                .keywords(List.of("JAVA"))
                                .build()
                )
        );

        given(userRepository.findByIdWithInterestCategories(userId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userCommandService.completeOnboarding(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(interestCommandService, never()).saveUserInterests(any(), any());
    }

    @Test
    @DisplayName("온보딩 완료 - description이 null이어도 정상 처리")
    void completeOnboarding_NullDescription_Success() {
        // Given
        Long userId = 1L;
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com");

        OnboardingRequest request = new OnboardingRequest(
                "테크포크유저",
                "user@techfork.com",
                null,
                List.of(
                        UserInterestDto.builder()
                                .category("FRONTEND")
                                .keywords(List.of("REACT", "TYPESCRIPT"))
                                .build()
                )
        );

        given(userRepository.findByIdWithInterestCategories(userId))
                .willReturn(Optional.of(mockUser));

        // When
        userCommandService.completeOnboarding(userId, request);

        // Then
        assertThat(mockUser.getNickName()).isEqualTo("테크포크유저");
        assertThat(mockUser.getEmail()).isEqualTo("user@techfork.com");
        assertThat(mockUser.getDescription()).isNull();

        verify(interestCommandService, times(1)).saveUserInterests(eq(mockUser), any(SaveInterestRequest.class));
    }

    @Test
    @DisplayName("온보딩 완료 - 여러 카테고리와 키워드 조합")
    void completeOnboarding_MultipleCategories_Success() {
        // Given
        Long userId = 1L;
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com");

        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("BACKEND")
                        .keywords(List.of("JAVA", "SPRING", "PYTHON"))
                        .build(),
                UserInterestDto.builder()
                        .category("DEVOPS")
                        .keywords(List.of("DOCKER", "KUBERNETES"))
                        .build(),
                UserInterestDto.builder()
                        .category("DATABASE")
                        .keywords(List.of("MYSQL", "POSTGRESQL", "REDIS"))
                        .build()
        );

        OnboardingRequest request = new OnboardingRequest(
                "풀스택개발자",
                "fullstack@techfork.com",
                "백엔드와 인프라를 다룹니다",
                interests
        );

        given(userRepository.findByIdWithInterestCategories(userId))
                .willReturn(Optional.of(mockUser));

        // When
        userCommandService.completeOnboarding(userId, request);

        // Then
        assertThat(mockUser.getNickName()).isEqualTo("풀스택개발자");
        verify(interestCommandService, times(1)).saveUserInterests(eq(mockUser), any(SaveInterestRequest.class));
    }
}
