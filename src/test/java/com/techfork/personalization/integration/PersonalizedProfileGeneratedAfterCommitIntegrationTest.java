package com.techfork.personalization.integration;

import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.global.common.IntegrationTestBase;
import com.techfork.personalization.application.event.PersonalizedProfileGeneratedEvent;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.useraccount.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PersonalizedProfileGeneratedAfterCommitIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private UserLookupService userLookupService;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    @DisplayName("프로필 생성 이벤트는 실제 트랜잭션 커밋 이후 추천 생성을 실행한다")
    void profileGeneratedEvent_CommittedTransaction_GeneratesRecommendationAfterCommit() {
        Long userId = 1L;
        User user = mock(User.class);
        float[] profileVector = new float[]{0.1f, 0.2f};
        List<String> keyKeywords = List.of("Spring", "JPA");
        given(userLookupService.getUserOrThrow(userId)).willReturn(user);
        given(recommendationService.generateRecommendationsForUser(
                eq(user),
                argThat(vector -> Arrays.equals(vector, profileVector)),
                eq(keyKeywords)
        )).willReturn(5);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new PersonalizedProfileGeneratedEvent(userId, profileVector, keyKeywords));

            verifyNoInteractions(userLookupService, recommendationService);
        });

        verify(userLookupService).getUserOrThrow(userId);
        verify(recommendationService).generateRecommendationsForUser(
                eq(user),
                argThat(vector -> Arrays.equals(vector, profileVector)),
                eq(keyKeywords)
        );
        verify(recommendationService, never()).generateRecommendationsForUser(user);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 프로필 생성 이벤트의 추천 후처리는 실행되지 않는다")
    void profileGeneratedEvent_RolledBackTransaction_DoesNotGenerateRecommendation() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new PersonalizedProfileGeneratedEvent(
                    1L,
                    new float[]{0.1f},
                    List.of("Spring")
            ));
            status.setRollbackOnly();
        });

        verifyNoInteractions(userLookupService, recommendationService);
    }
}
