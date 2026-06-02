package com.techfork.useraccount.application.command;

import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.application.command.input.CompleteOnboardingCommand;
import com.techfork.useraccount.application.command.input.UpdateAccountProfileCommand;
import com.techfork.useraccount.application.command.input.UserInterestCommand;
import com.techfork.useraccount.application.command.input.WithdrawUserCommand;
import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserWithdrawnEvent;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private InterestCommandService interestCommandService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserCommandService userCommandService;

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
    @DisplayName("온보딩 완료 - 정상 케이스")
    void completeOnboarding_Success() {
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("JAVA", "SPRING")).build(),
                UserInterestCommand.builder().category("DATABASE").keywords(List.of("MYSQL", "REDIS")).build()
        );
        CompleteOnboardingCommand command = new CompleteOnboardingCommand(userId, "테크포크유저", "user@techfork.com", "백엔드 개발자입니다", interests);
        given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.of(mockUser));

        userCommandService.completeOnboarding(command);

        assertThat(mockUser.getNickName()).isEqualTo("테크포크유저");
        assertThat(mockUser.getEmail()).isEqualTo("user@techfork.com");
        assertThat(mockUser.getDescription()).isEqualTo("백엔드 개발자입니다");
        verify(userRepository).findByIdWithInterestCategories(userId);
        verify(interestCommandService).saveUserInterests(eq(mockUser), any());
        verifyPublishedEvent(OnboardingCompletedEvent.class, userId);
    }

    @Test
    @DisplayName("온보딩 완료 - 사용자가 존재하지 않으면 예외 발생")
    void completeOnboarding_UserNotFound_ThrowsException() {
        CompleteOnboardingCommand command = new CompleteOnboardingCommand(
                999L,
                "테크포크유저",
                "user@techfork.com",
                null,
                List.of(UserInterestCommand.builder().category("BACKEND").keywords(List.of("JAVA")).build())
        );
        given(userRepository.findByIdWithInterestCategories(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userCommandService.completeOnboarding(command))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(interestCommandService, never()).saveUserInterests(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("온보딩 완료 - description이 null이어도 정상 처리")
    void completeOnboarding_NullDescription_Success() {
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        CompleteOnboardingCommand command = new CompleteOnboardingCommand(
                userId,
                "테크포크유저",
                "user@techfork.com",
                null,
                List.of(UserInterestCommand.builder().category("FRONTEND").keywords(List.of("REACT", "TYPESCRIPT")).build())
        );
        given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.of(mockUser));

        userCommandService.completeOnboarding(command);

        assertThat(mockUser.getNickName()).isEqualTo("테크포크유저");
        assertThat(mockUser.getEmail()).isEqualTo("user@techfork.com");
        assertThat(mockUser.getDescription()).isNull();
        verify(interestCommandService).saveUserInterests(eq(mockUser), any());
        verifyPublishedEvent(OnboardingCompletedEvent.class, userId);
    }

    @Test
    @DisplayName("온보딩 완료 - 여러 카테고리와 키워드 조합")
    void completeOnboarding_MultipleCategories_Success() {
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("JAVA", "SPRING", "PYTHON")).build(),
                UserInterestCommand.builder().category("DEVOPS").keywords(List.of("DOCKER", "KUBERNETES")).build(),
                UserInterestCommand.builder().category("DATABASE").keywords(List.of("MYSQL", "POSTGRESQL", "REDIS")).build()
        );
        CompleteOnboardingCommand command = new CompleteOnboardingCommand(userId, "풀스택개발자", "fullstack@techfork.com", "백엔드와 인프라를 다룹니다", interests);
        given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.of(mockUser));

        userCommandService.completeOnboarding(command);

        assertThat(mockUser.getNickName()).isEqualTo("풀스택개발자");
        verify(interestCommandService).saveUserInterests(eq(mockUser), any());
        verifyPublishedEvent(OnboardingCompletedEvent.class, userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 성공 - 닉네임만 수정")
    void updateAccountProfile_Success_OnlyNickName() {
        UpdateAccountProfileCommand command = new UpdateAccountProfileCommand(userId, "새로운닉네임", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        userCommandService.updateAccountProfile(command);

        assertThat(testUser.getNickName()).isEqualTo("새로운닉네임");
        assertThat(testUser.getDescription()).isEqualTo("백엔드 개발자입니다.");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 성공 - 자기소개만 수정")
    void updateAccountProfile_Success_OnlyDescription() {
        UpdateAccountProfileCommand command = new UpdateAccountProfileCommand(userId, null, "새로운 자기소개");
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        userCommandService.updateAccountProfile(command);

        assertThat(testUser.getNickName()).isEqualTo("테스트유저");
        assertThat(testUser.getDescription()).isEqualTo("새로운 자기소개");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 성공 - 닉네임과 자기소개 모두 수정")
    void updateAccountProfile_Success_BothFields() {
        UpdateAccountProfileCommand command = new UpdateAccountProfileCommand(userId, "새닉네임", "새 자기소개");
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        userCommandService.updateAccountProfile(command);

        assertThat(testUser.getNickName()).isEqualTo("새닉네임");
        assertThat(testUser.getDescription()).isEqualTo("새 자기소개");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 성공 - 아무것도 수정하지 않음")
    void updateAccountProfile_Success_NoChanges() {
        UpdateAccountProfileCommand command = new UpdateAccountProfileCommand(userId, null, null);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        userCommandService.updateAccountProfile(command);

        assertThat(testUser.getNickName()).isEqualTo("테스트유저");
        assertThat(testUser.getDescription()).isEqualTo("백엔드 개발자입니다.");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("계정 프로필 수정 실패 - 사용자를 찾을 수 없음")
    void updateAccountProfile_Fail_UserNotFound() {
        UpdateAccountProfileCommand command = new UpdateAccountProfileCommand(userId, "새닉네임", "새 자기소개");
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userCommandService.updateAccountProfile(command))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 개인정보 익명화 확인")
    void withdrawUser_Success() {
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        String originalSocialId = testUser.getSocialId();

        userCommandService.withdrawUser(new WithdrawUserCommand(userId));

        assertThat(testUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(testUser.isWithdrawn()).isTrue();
        assertThat(testUser.getNickName()).isNull();
        assertThat(testUser.getEmail()).isNull();
        assertThat(testUser.getProfileImage()).isNull();
        assertThat(testUser.getDescription()).isNull();
        assertThat(testUser.getSocialId()).isEqualTo(originalSocialId);
        verify(userRepository).findById(userId);
        verifyPublishedEvent(UserWithdrawnEvent.class, userId);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 사용자를 찾을 수 없음")
    void withdrawUser_Fail_UserNotFound() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userCommandService.withdrawUser(new WithdrawUserCommand(userId)))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        verify(userRepository).findById(userId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 이미 탈퇴한 회원")
    void withdrawUser_Fail_AlreadyWithdrawn() {
        testUser.withdraw();
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userCommandService.withdrawUser(new WithdrawUserCommand(userId)))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(UserErrorCode.ALREADY_WITHDRAWN);
        verify(userRepository).findById(userId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    private void verifyPublishedEvent(Class<?> eventType, Long expectedUserId) {
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue()).isInstanceOf(eventType);
        Long actualUserId = extractUserId(eventCaptor.getValue());
        assertThat(actualUserId).isEqualTo(expectedUserId);
    }

    private Long extractUserId(Object event) {
        if (event instanceof OnboardingCompletedEvent onboardingCompletedEvent) {
            return onboardingCompletedEvent.userId();
        }
        if (event instanceof UserWithdrawnEvent userWithdrawnEvent) {
            return userWithdrawnEvent.userId();
        }
        throw new IllegalArgumentException("지원하지 않는 이벤트 타입입니다: " + event.getClass());
    }
}
