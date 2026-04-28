package com.techfork.domain.user.service;

import com.techfork.domain.user.dto.OnboardingRequest;
import com.techfork.domain.user.dto.SaveInterestRequest;
import com.techfork.domain.user.dto.UpdateAccountProfileRequest;
import com.techfork.domain.user.dto.UserInterestDto;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.security.auth.service.UserAuthCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private UserAuthCacheService userAuthCacheService;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    @DisplayName("온보딩 완료 - 정상 케이스")
    void completeOnboarding_Success() {
        // Given
        Long userId = 1L;
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

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
        verify(userAuthCacheService).evict(userId);
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
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

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
        verify(userAuthCacheService).evict(userId);
    }

    @Test
    @DisplayName("온보딩 완료 - 여러 카테고리와 키워드 조합")
    void completeOnboarding_MultipleCategories_Success() {
        // Given
        Long userId = 1L;
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

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
        verify(userAuthCacheService).evict(userId);
    }

    // ===== 프로필 수정 테스트 =====

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
    @DisplayName("계정 프로필 수정 성공 - 닉네임만 수정")
    void updateAccountProfile_Success_OnlyNickName() {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest("새로운닉네임", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // When
        userCommandService.updateAccountProfile(userId, request);

        // Then
        assertThat(testUser.getNickName()).isEqualTo("새로운닉네임");
        assertThat(testUser.getDescription()).isEqualTo("백엔드 개발자입니다."); // 변경되지 않음

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 성공 - 자기소개만 수정")
    void updateAccountProfile_Success_OnlyDescription() {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest(null, "새로운 자기소개");
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // When
        userCommandService.updateAccountProfile(userId, request);

        // Then
        assertThat(testUser.getNickName()).isEqualTo("테스트유저"); // 변경되지 않음
        assertThat(testUser.getDescription()).isEqualTo("새로운 자기소개");

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 성공 - 닉네임과 자기소개 모두 수정")
    void updateAccountProfile_Success_BothFields() {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest("새닉네임", "새 자기소개");
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // When
        userCommandService.updateAccountProfile(userId, request);

        // Then
        assertThat(testUser.getNickName()).isEqualTo("새닉네임");
        assertThat(testUser.getDescription()).isEqualTo("새 자기소개");

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 성공 - 아무것도 수정하지 않음")
    void updateAccountProfile_Success_NoChanges() {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest(null, null);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // When
        userCommandService.updateAccountProfile(userId, request);

        // Then
        assertThat(testUser.getNickName()).isEqualTo("테스트유저"); // 변경되지 않음
        assertThat(testUser.getDescription()).isEqualTo("백엔드 개발자입니다."); // 변경되지 않음

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 실패 - 사용자를 찾을 수 없음")
    void updateAccountProfile_Fail_UserNotFound() {
        // Given
        UpdateAccountProfileRequest request = new UpdateAccountProfileRequest("새닉네임", "새 자기소개");
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userCommandService.updateAccountProfile(userId, request))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
    }

    // ===== 회원 탈퇴 테스트 =====

    @Test
    @DisplayName("회원 탈퇴 성공 - 개인정보 익명화 확인")
    void withdrawUser_Success() {
        // Given
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        String originalSocialId = testUser.getSocialId();

        // When
        userCommandService.withdrawUser(userId);

        // Then
        assertThat(testUser.getStatus()).isEqualTo(com.techfork.domain.user.enums.UserStatus.WITHDRAWN);
        assertThat(testUser.isWithdrawn()).isTrue();

        // 개인정보 익명화 확인
        assertThat(testUser.getNickName()).isNull();
        assertThat(testUser.getEmail()).isNull();
        assertThat(testUser.getProfileImage()).isNull();
        assertThat(testUser.getDescription()).isNull();

        // socialId는 유지
        assertThat(testUser.getSocialId()).isEqualTo(originalSocialId);

        verify(userRepository).findById(userId);
        verify(userAuthCacheService).evict(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 사용자를 찾을 수 없음")
    void withdrawUser_Fail_UserNotFound() {
        // Given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userCommandService.withdrawUser(userId))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 이미 탈퇴한 회원")
    void withdrawUser_Fail_AlreadyWithdrawn() {
        // Given
        testUser.withdraw(); // 이미 탈퇴 처리
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userCommandService.withdrawUser(userId))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(UserErrorCode.ALREADY_WITHDRAWN);

        verify(userRepository).findById(userId);
    }
}
