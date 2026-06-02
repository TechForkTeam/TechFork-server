package com.techfork.global.security.auth.listener;

import com.techfork.global.security.auth.service.UserAuthCacheService;
import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
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
    @DisplayName("회원 탈퇴 이벤트 - 커밋 이후 인증 캐시를 무효화한다")
    void handle_UserWithdrawnEvent_EvictsUserAuthCache() {
        Long userId = 1L;

        listener.handle(new UserWithdrawnEvent(userId));

        verify(userAuthCacheService).evict(userId);
    }

    @Test
    @DisplayName("인증 캐시 리스너는 AFTER_COMMIT 단계에서 실행된다")
    void listenerMethods_RunAfterCommit() throws NoSuchMethodException {
        assertAfterCommitListener("handle", OnboardingCompletedEvent.class);
        assertAfterCommitListener("handle", UserWithdrawnEvent.class);
    }

    private void assertAfterCommitListener(String methodName, Class<?> eventType) throws NoSuchMethodException {
        TransactionalEventListener annotation = UserAuthCacheEventListener.class
                .getDeclaredMethod(methodName, eventType)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
