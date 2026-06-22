package com.techfork.personalization.application;

import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserInterestsChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class PersonalizationProfileEventListenerTest {

    @Mock
    private PersonalizationProfileService personalizationProfileService;

    @InjectMocks
    private PersonalizationProfileEventListener listener;

    @Nested
    @DisplayName("도메인 이벤트 처리")
    class HandleDomainEvent {

        @Test
        @DisplayName("온보딩 완료 이벤트 - 커밋 이후 개인화 프로필 생성을 요청한다")
        void onboardingCompletedEvent_GeneratesPersonalizationProfile() {
            Long userId = 1L;

            listener.handle(new OnboardingCompletedEvent(userId));

            verify(personalizationProfileService).generatePersonalizationProfile(userId);
        }

        @Test
        @DisplayName("관심사 변경 이벤트 - 커밋 이후 개인화 프로필 생성을 요청한다")
        void userInterestsChangedEvent_GeneratesPersonalizationProfile() {
            Long userId = 1L;

            listener.handle(new UserInterestsChangedEvent(userId));

            verify(personalizationProfileService).generatePersonalizationProfile(userId);
        }
    }

    @Nested
    @DisplayName("transaction phase")
    class TransactionPhaseContract {

        @Test
        @DisplayName("개인화 프로필 리스너는 AFTER_COMMIT 단계에서 실행된다")
        void listeners_RunAfterCommit() throws NoSuchMethodException {
            assertAfterCommitListener("handle", OnboardingCompletedEvent.class);
            assertAfterCommitListener("handle", UserInterestsChangedEvent.class);
        }
    }

    private void assertAfterCommitListener(String methodName, Class<?> eventType) throws NoSuchMethodException {
        TransactionalEventListener annotation = PersonalizationProfileEventListener.class
                .getDeclaredMethod(methodName, eventType)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
