package com.techfork.auth.security.cache;

import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserReactivatedEvent;
import com.techfork.useraccount.application.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthCacheInvalidationListener {

    private static final String ONBOARDING_COMPLETED_AFTER_COMMIT = "onboarding-completed-after-commit";
    private static final String USER_REACTIVATED_AFTER_COMMIT = "user-reactivated-after-commit";
    private static final String USER_WITHDRAWN_BEFORE_COMMIT = "user-withdrawn-before-commit";
    private static final String USER_WITHDRAWN_AFTER_COMMIT = "user-withdrawn-after-commit";

    private final UserAuthCacheStore userAuthCacheStore;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evictOnOnboardingCompletedAfterCommit(OnboardingCompletedEvent event) {
        evictUserAuthCache(event.userId(), ONBOARDING_COMPLETED_AFTER_COMMIT);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evictOnUserReactivatedAfterCommit(UserReactivatedEvent event) {
        evictUserAuthCache(event.userId(), USER_REACTIVATED_AFTER_COMMIT);
    }

    /**
     * 탈퇴 전 캐시 무효화는 탈퇴 사용자 인증 차단을 위한 커밋 게이트다.
     * 실패하면 탈퇴 트랜잭션을 롤백해야 하므로 예외를 잡거나 완화하지 않는다.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void evictOnUserWithdrawnBeforeCommit(UserWithdrawnEvent event) {
        evictUserAuthCache(event.userId(), USER_WITHDRAWN_BEFORE_COMMIT);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evictOnUserWithdrawnAfterCommit(UserWithdrawnEvent event) {
        evictUserAuthCache(event.userId(), USER_WITHDRAWN_AFTER_COMMIT);
    }

    private void evictUserAuthCache(Long userId, String reason) {
        userAuthCacheStore.evict(userId);
        log.info("User auth cache eviction requested - userId: {}, reason: {}", userId, reason);
    }
}
