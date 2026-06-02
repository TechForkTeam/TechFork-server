package com.techfork.useraccount.integration;

import com.techfork.domain.personalization.service.PersonalizationProfileService;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.global.security.auth.service.UserAuthCacheService;
import com.techfork.useraccount.application.command.InterestCommandService;
import com.techfork.useraccount.application.command.UserCommandService;
import com.techfork.useraccount.application.command.input.CompleteOnboardingCommand;
import com.techfork.useraccount.application.command.input.UpdateUserInterestsCommand;
import com.techfork.useraccount.application.command.input.UserInterestCommand;
import com.techfork.useraccount.application.command.input.WithdrawUserCommand;
import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserInterestsChangedEvent;
import com.techfork.useraccount.application.event.UserWithdrawnEvent;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.useraccount.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserAccountAfterCommitEventIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private InterestCommandService interestCommandService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private PersonalizationProfileService personalizationProfileService;

    @MockitoBean
    private UserAuthCacheService userAuthCacheService;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("온보딩 완료 이벤트는 실제 트랜잭션 커밋 이후 인증 캐시 무효화와 개인화 프로필 생성을 실행한다")
    void completeOnboarding_AfterCommit_EvictsAuthCacheAndGeneratesPersonalizationProfile() {
        User user = savePendingUser();
        CompleteOnboardingCommand command = new CompleteOnboardingCommand(
                user.getId(),
                "테크포크유저",
                "user@techfork.com",
                "백엔드 개발자입니다",
                List.of(backendInterest())
        );

        userCommandService.completeOnboarding(command);

        verify(userAuthCacheService).evict(user.getId());
        verify(personalizationProfileService).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 수정 이벤트는 실제 트랜잭션 커밋 이후 개인화 프로필 생성만 실행한다")
    void updateUserInterests_AfterCommit_GeneratesPersonalizationProfileOnly() {
        User user = saveActiveUser();
        UpdateUserInterestsCommand command = new UpdateUserInterestsCommand(
                user.getId(),
                List.of(backendInterest())
        );

        interestCommandService.updateUserInterests(command);

        verify(personalizationProfileService).generatePersonalizationProfile(user.getId());
        verifyNoInteractions(userAuthCacheService);
    }

    @Test
    @DisplayName("회원 탈퇴 이벤트는 실제 트랜잭션 커밋 전후로 인증 캐시 무효화만 실행한다")
    void withdrawUser_BeforeAndAfterCommit_EvictsAuthCacheOnly() {
        User user = saveActiveUser();

        userCommandService.withdrawUser(new WithdrawUserCommand(user.getId()));

        verify(userAuthCacheService, times(2)).evict(user.getId());
        verifyNoInteractions(personalizationProfileService);
    }

    @Test
    @DisplayName("회원 탈퇴 커밋 직전 인증 캐시 무효화에 실패하면 탈퇴 트랜잭션을 롤백한다")
    void withdrawUser_BeforeCommitCacheEvictionFails_RollsBackWithdrawal() {
        User user = saveActiveUser();
        doThrow(new RuntimeException("redis eviction failed"))
                .when(userAuthCacheService)
                .evict(user.getId());

        assertThatThrownBy(() -> userCommandService.withdrawUser(new WithdrawUserCommand(user.getId())))
                .isInstanceOf(RuntimeException.class);

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userAuthCacheService).evict(user.getId());
        verifyNoInteractions(personalizationProfileService);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 발행된 사용자 이벤트의 트랜잭션 후처리는 실행되지 않는다")
    void publishedUserEvents_RolledBack_DoNotRunTransactionalHandlers() {
        Long userId = 1L;

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OnboardingCompletedEvent(userId));
            eventPublisher.publishEvent(new UserInterestsChangedEvent(userId));
            eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
            status.setRollbackOnly();
        });

        verifyNoInteractions(userAuthCacheService, personalizationProfileService);
    }

    private User savePendingUser() {
        User user = User.createSocialUser(SocialType.KAKAO, uniqueSocialId(), "pending@example.com", null);
        return userRepository.save(user);
    }

    private User saveActiveUser() {
        User user = User.createSocialUser(SocialType.KAKAO, uniqueSocialId(), "active@example.com", null);
        user.updateUser("테스트유저", "active@example.com", "백엔드 개발자입니다");
        return userRepository.save(user);
    }

    private UserInterestCommand backendInterest() {
        return UserInterestCommand.builder()
                .category("BACKEND")
                .keywords(List.of("JAVA", "SPRING"))
                .build();
    }

    private String uniqueSocialId() {
        return "test-" + UUID.randomUUID();
    }
}
