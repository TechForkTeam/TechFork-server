package com.techfork.domain.personalization.listener;

import com.techfork.domain.personalization.service.PersonalizationProfileService;
import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserInterestsChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalizationProfileEventListener {

    private final PersonalizationProfileService personalizationProfileService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OnboardingCompletedEvent event) {
        generatePersonalizationProfile(event.userId(), "onboarding-completed");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserInterestsChangedEvent event) {
        generatePersonalizationProfile(event.userId(), "interests-changed");
    }

    private void generatePersonalizationProfile(Long userId, String reason) {
        personalizationProfileService.generatePersonalizationProfile(userId);
        log.info("Personalization profile generation requested after commit - userId: {}, reason: {}", userId, reason);
    }
}
