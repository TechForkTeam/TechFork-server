package com.techfork.global.security.auth.listener;

import com.techfork.global.security.auth.service.UserAuthCacheService;
import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthCacheEventListener {

    private final UserAuthCacheService userAuthCacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OnboardingCompletedEvent event) {
        evictUserAuthCache(event.userId(), "onboarding-completed");
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleBeforeCommit(UserWithdrawnEvent event) {
        evictUserAuthCache(event.userId(), "user-withdrawn-before-commit");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(UserWithdrawnEvent event) {
        evictUserAuthCache(event.userId(), "user-withdrawn-after-commit");
    }

    private void evictUserAuthCache(Long userId, String reason) {
        userAuthCacheService.evict(userId);
        log.info("User auth cache eviction requested - userId: {}, reason: {}", userId, reason);
    }
}
