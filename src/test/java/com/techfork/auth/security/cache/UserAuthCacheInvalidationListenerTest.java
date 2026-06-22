package com.techfork.auth.security.cache;

import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserReactivatedEvent;
import com.techfork.useraccount.application.event.UserWithdrawnEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAuthCacheInvalidationListenerTest {

    @Mock
    private UserAuthCacheStore userAuthCacheStore;

    @InjectMocks
    private UserAuthCacheInvalidationListener listener;

    @Nested
    @DisplayName("온보딩 완료 이벤트")
    class OnboardingCompleted {

        @Test
        @DisplayName("커밋 이후 인증 캐시를 무효화한다")
        void afterCommit_EvictsUserAuthCache() {
            Long userId = 1L;

            listener.evictOnOnboardingCompletedAfterCommit(new OnboardingCompletedEvent(userId));

            verify(userAuthCacheStore).evict(userId);
        }

        @Test
        @DisplayName("AFTER_COMMIT 단계에서만 실행한다")
        void listenerPhase_RunsAfterCommitOnly() {
            assertTransactionalEventListenerPhases(OnboardingCompletedEvent.class, TransactionPhase.AFTER_COMMIT);
        }
    }

    @Nested
    @DisplayName("회원 재활성화 이벤트")
    class UserReactivated {

        @Test
        @DisplayName("커밋 이후 인증 캐시를 무효화한다")
        void afterCommit_EvictsUserAuthCache() {
            Long userId = 1L;

            listener.evictOnUserReactivatedAfterCommit(new UserReactivatedEvent(userId));

            verify(userAuthCacheStore).evict(userId);
        }

        @Test
        @DisplayName("AFTER_COMMIT 단계에서만 실행한다")
        void listenerPhase_RunsAfterCommitOnly() {
            assertTransactionalEventListenerPhases(UserReactivatedEvent.class, TransactionPhase.AFTER_COMMIT);
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 이벤트")
    class UserWithdrawn {

        @Test
        @DisplayName("커밋 직전 인증 캐시를 무효화한다")
        void beforeCommit_EvictsUserAuthCache() {
            Long userId = 1L;

            listener.evictOnUserWithdrawnBeforeCommit(new UserWithdrawnEvent(userId));

            verify(userAuthCacheStore).evict(userId);
        }

        @Test
        @DisplayName("커밋 이후 인증 캐시를 한 번 더 무효화한다")
        void afterCommit_EvictsUserAuthCache() {
            Long userId = 1L;

            listener.evictOnUserWithdrawnAfterCommit(new UserWithdrawnEvent(userId));

            verify(userAuthCacheStore).evict(userId);
        }

        @Test
        @DisplayName("BEFORE_COMMIT과 AFTER_COMMIT 단계에서 모두 실행한다")
        void listenerPhase_RunsBeforeAndAfterCommit() {
            assertTransactionalEventListenerPhases(
                    UserWithdrawnEvent.class,
                    TransactionPhase.BEFORE_COMMIT,
                    TransactionPhase.AFTER_COMMIT
            );
        }

        @Test
        @DisplayName("커밋 직전 인증 캐시 무효화 실패는 예외를 전파한다")
        void beforeCommitEvictionFails_PropagatesException() {
            Long userId = 1L;
            RuntimeException evictionFailure = new RuntimeException("redis eviction failed");
            doThrow(evictionFailure).when(userAuthCacheStore).evict(userId);

            assertThatThrownBy(() -> listener.evictOnUserWithdrawnBeforeCommit(new UserWithdrawnEvent(userId)))
                    .isSameAs(evictionFailure);
        }
    }

    private void assertTransactionalEventListenerPhases(
            Class<?> eventType,
            TransactionPhase... expectedPhases
    ) {
        List<TransactionalEventListener> annotations = Arrays.stream(UserAuthCacheInvalidationListener.class.getDeclaredMethods())
                .filter(method -> listensToEvent(method, eventType))
                .map(method -> method.getAnnotation(TransactionalEventListener.class))
                .toList();

        assertThat(annotations)
                .extracting(TransactionalEventListener::phase)
                .containsExactlyInAnyOrder(expectedPhases);
        assertThat(annotations)
                .allSatisfy(annotation -> assertThat(annotation.fallbackExecution()).isFalse());
    }

    private boolean listensToEvent(Method method, Class<?> eventType) {
        return method.isAnnotationPresent(TransactionalEventListener.class)
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].equals(eventType);
    }
}
