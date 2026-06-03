package com.techfork.domain.recommendation.listener;

import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.personalization.application.event.PersonalizedProfileGeneratedEvent;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import com.techfork.useraccount.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PersonalizedProfileGeneratedEventListenerTest {

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private PersonalizedProfileGeneratedEventListener listener;

    @Test
    @DisplayName("프로필 생성 이벤트를 받으면 추천을 생성한다")
    void handle_GeneratesRecommendationsWhenProfileGeneratedEventIsReceived() {
        Long userId = 1L;
        User user = mock(User.class);
        float[] profileVector = new float[]{0.1f, 0.2f};
        List<String> keyKeywords = List.of("Spring", "JPA");
        given(userLookupService.getUserOrThrow(userId)).willReturn(user);
        given(recommendationService.generateRecommendationsForUser(
                eq(user),
                any(float[].class),
                eq(keyKeywords)
        )).willReturn(5);

        listener.handle(new PersonalizedProfileGeneratedEvent(userId, profileVector, keyKeywords));

        verify(userLookupService).getUserOrThrow(userId);
        verify(recommendationService).generateRecommendationsForUser(
                eq(user),
                argThat(vector -> Arrays.equals(vector, profileVector)),
                eq(keyKeywords)
        );
    }

    @Test
    @DisplayName("추천 생성이 실패해도 예외를 전파하지 않는다")
    void handle_RecommendationFailureDoesNotPropagateException() {
        Long userId = 2L;
        User user = mock(User.class);
        float[] profileVector = new float[]{0.1f, 0.2f};
        List<String> keyKeywords = List.of("Spring", "JPA");
        given(userLookupService.getUserOrThrow(userId)).willReturn(user);
        given(recommendationService.generateRecommendationsForUser(
                eq(user),
                any(float[].class),
                eq(keyKeywords)
        ))
                .willThrow(new RuntimeException("recommendation failure"));

        assertThatCode(() -> listener.handle(new PersonalizedProfileGeneratedEvent(userId, profileVector, keyKeywords)))
                .doesNotThrowAnyException();

        verify(userLookupService).getUserOrThrow(userId);
        verify(recommendationService).generateRecommendationsForUser(
                eq(user),
                argThat(vector -> Arrays.equals(vector, profileVector)),
                eq(keyKeywords)
        );
    }

    @Test
    @DisplayName("사용자 조회가 실패해도 예외를 전파하지 않고 추천을 시도하지 않는다")
    void handle_UserLookupFailureDoesNotPropagateExceptionAndSkipsRecommendation() {
        Long userId = 3L;
        given(userLookupService.getUserOrThrow(userId))
                .willThrow(new RuntimeException("user lookup failure"));

        assertThatCode(() -> listener.handle(new PersonalizedProfileGeneratedEvent(userId, new float[]{0.1f}, List.of("Spring"))))
                .doesNotThrowAnyException();

        verify(userLookupService).getUserOrThrow(userId);
        verifyNoInteractions(recommendationService);
    }

    @Test
    @DisplayName("프로필 생성 이벤트 리스너는 AFTER_COMMIT 단계에서 실행된다")
    void listenerMethod_RunsAfterCommit() throws NoSuchMethodException {
        TransactionalEventListener annotation = PersonalizedProfileGeneratedEventListener.class
                .getDeclaredMethod("handle", PersonalizedProfileGeneratedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
