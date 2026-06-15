package com.techfork.auth.security.listener;

import com.techfork.auth.security.service.UserAuthCacheService;
import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserReactivatedEvent;
import com.techfork.useraccount.application.event.UserWithdrawnEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAuthCacheEventListenerTest {

    @Mock
    private UserAuthCacheService userAuthCacheService;

    @InjectMocks
    private UserAuthCacheEventListener listener;

    @Test
    @DisplayName("온보딩 완료 이벤트 - 커밋 이후 인증 캐시를 무효화한다")
    void handle_OnboardingCompletedEvent_EvictsUserAuthCache() {
        Long userId = 1L;

        listener.handle(new OnboardingCompletedEvent(userId));

        verify(userAuthCacheService).evict(userId);
    }

    @Test
    @DisplayName("회원 재활성화 이벤트 - 커밋 이후 인증 캐시를 무효화한다")
    void handle_UserReactivatedEvent_EvictsUserAuthCache() {
        Long userId = 1L;

        listener.handle(new UserReactivatedEvent(userId));

        verify(userAuthCacheService).evict(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 이벤트 - 커밋 직전 인증 캐시를 무효화한다")
    void handleBeforeCommit_UserWithdrawnEvent_EvictsUserAuthCache() {
        Long userId = 1L;

        listener.handleBeforeCommit(new UserWithdrawnEvent(userId));

        verify(userAuthCacheService).evict(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 이벤트 - 커밋 이후 인증 캐시를 한 번 더 무효화한다")
    void handleAfterCommit_UserWithdrawnEvent_EvictsUserAuthCache() {
        Long userId = 1L;

        listener.handleAfterCommit(new UserWithdrawnEvent(userId));

        verify(userAuthCacheService).evict(userId);
    }

    @Test
    @DisplayName("인증 캐시 리스너는 이벤트별 트랜잭션 단계를 명시한다")
    void listenerMethods_RunWithExpectedTransactionPhase() throws NoSuchMethodException {
        assertTransactionalEventListenerPhase("handle", OnboardingCompletedEvent.class, TransactionPhase.AFTER_COMMIT);
        assertTransactionalEventListenerPhase("handle", UserReactivatedEvent.class, TransactionPhase.AFTER_COMMIT);
        assertTransactionalEventListenerPhase("handleBeforeCommit", UserWithdrawnEvent.class, TransactionPhase.BEFORE_COMMIT);
        assertTransactionalEventListenerPhase("handleAfterCommit", UserWithdrawnEvent.class, TransactionPhase.AFTER_COMMIT);
    }

    private void assertTransactionalEventListenerPhase(
            String methodName,
            Class<?> eventType,
            TransactionPhase expectedPhase
    ) throws NoSuchMethodException {
        TransactionalEventListener annotation = UserAuthCacheEventListener.class
                .getDeclaredMethod(methodName, eventType)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(expectedPhase);
    }
}
