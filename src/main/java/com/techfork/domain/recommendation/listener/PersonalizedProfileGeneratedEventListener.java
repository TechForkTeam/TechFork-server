package com.techfork.domain.recommendation.listener;

import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.personalization.application.event.PersonalizedProfileGeneratedEvent;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.useraccount.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalizedProfileGeneratedEventListener {

    private final UserLookupService userLookupService;
    private final RecommendationService recommendationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PersonalizedProfileGeneratedEvent event) {
        Long userId = event.userId();

        try {
            User user = userLookupService.getUserOrThrow(userId);
            int recommendationCount = recommendationService.generateRecommendationsForUser(
                    user,
                    event.profileVector(),
                    event.keyKeywords()
            );

            log.info("Recommendations generated after personalization profile creation for userId: {} - {} recommendations created",
                    userId, recommendationCount);
        } catch (Exception e) {
            log.error("Failed to generate recommendations after personalization profile creation for userId: {}", userId, e);
        }
    }
}
