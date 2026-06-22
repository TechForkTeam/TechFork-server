package com.techfork.useraccount.integration;

import com.techfork.personalization.application.PersonalizationProfileService;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.auth.security.cache.UserAuthCacheStore;
import com.techfork.useraccount.application.command.InterestCommandService;
import com.techfork.useraccount.application.command.UserCommandService;
import com.techfork.useraccount.application.command.input.CompleteOnboardingCommand;
import com.techfork.useraccount.application.command.input.UpdateUserInterestsCommand;
import com.techfork.useraccount.application.command.input.UserInterestCommand;
import com.techfork.useraccount.application.command.input.WithdrawUserCommand;
import com.techfork.useraccount.application.auth.UserAuthAccountService;
import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserInterestsChangedEvent;
import com.techfork.useraccount.application.event.UserReactivatedEvent;
import com.techfork.useraccount.application.event.UserWithdrawnEvent;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.useraccount.fixture.UserFixture;
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
    private UserAuthAccountService userAuthAccountService;

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
    private UserAuthCacheStore userAuthCacheStore;

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

        verify(userAuthCacheStore).evict(user.getId());
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
        verifyNoInteractions(userAuthCacheStore);
    }

    @Test
    @DisplayName("회원 탈퇴 이벤트는 실제 트랜잭션 커밋 전후로 인증 캐시 무효화만 실행한다")
    void withdrawUser_BeforeAndAfterCommit_EvictsAuthCacheOnly() {
        User user = saveActiveUser();

        userCommandService.withdrawUser(new WithdrawUserCommand(user.getId()));

        verify(userAuthCacheStore, times(2)).evict(user.getId());
        verifyNoInteractions(personalizationProfileService);
    }

    @Test
    @DisplayName("회원 재활성화 이벤트는 실제 트랜잭션 커밋 이후 인증 캐시 무효화만 실행한다")
    void reactivateUser_AfterCommit_EvictsAuthCacheOnly() {
        User user = saveWithdrawnUser();

        userAuthAccountService.getOrCreateSocialAuthProfile(
                user.getSocialType(),
                user.getSocialId(),
                "reactivated@example.com",
                "https://cdn.example.com/reactivated.png"
        );

        verify(userAuthCacheStore).evict(user.getId());
        verifyNoInteractions(personalizationProfileService);
    }

    @Test
    @DisplayName("회원 탈퇴 커밋 직전 인증 캐시 무효화에 실패하면 탈퇴 트랜잭션을 롤백한다")
    void withdrawUser_BeforeCommitCacheEvictionFails_RollsBackWithdrawal() {
        User user = saveActiveUser();
        doThrow(new RuntimeException("redis eviction failed"))
                .when(userAuthCacheStore)
                .evict(user.getId());

        assertThatThrownBy(() -> userCommandService.withdrawUser(new WithdrawUserCommand(user.getId())))
                .isInstanceOf(RuntimeException.class);

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getNickName()).isEqualTo("테스트유저");
        assertThat(savedUser.getEmail()).isEqualTo("active@example.com");
        assertThat(savedUser.getDescription()).isEqualTo("백엔드 개발자입니다");
        verify(userAuthCacheStore).evict(user.getId());
        verifyNoInteractions(personalizationProfileService);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 발행된 사용자 이벤트의 트랜잭션 후처리는 실행되지 않는다")
    void publishedUserEvents_RolledBack_DoNotRunTransactionalHandlers() {
        Long userId = 1L;

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OnboardingCompletedEvent(userId));
            eventPublisher.publishEvent(new UserInterestsChangedEvent(userId));
            eventPublisher.publishEvent(new UserReactivatedEvent(userId));
            eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
            status.setRollbackOnly();
        });

        verifyNoInteractions(userAuthCacheStore, personalizationProfileService);
    }

    private User savePendingUser() {
        User user = UserFixture.socialUser(uniqueSocialId(), "pending@example.com", null);
        return userRepository.save(user);
    }

    private User saveActiveUser() {
        User user = UserFixture.activeUser(uniqueSocialId(), "active@example.com");
        user.updateProfile("테스트유저", "백엔드 개발자입니다");
        return userRepository.save(user);
    }

    private User saveWithdrawnUser() {
        User user = saveActiveUser();
        user.withdraw();
        return userRepository.saveAndFlush(user);
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
