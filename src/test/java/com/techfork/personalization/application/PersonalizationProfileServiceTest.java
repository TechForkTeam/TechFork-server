package com.techfork.personalization.application;

import com.techfork.personalization.application.event.PersonalizedProfileGeneratedEvent;
import com.techfork.personalization.application.generation.PersonalizedProfileGenerator;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocument;
import com.techfork.personalization.fixture.PersonalizationProfileDocumentFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PersonalizationProfileServiceTest {

    @Mock
    private PersonalizedProfileGenerator personalizedProfileGenerator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PersonalizationProfileService personalizationProfileService;

    @Test
    @DisplayName("개인화 프로필 생성 성공 후 프로필 생성 이벤트를 발행한다")
    void generatePersonalizationProfileSync_PublishesProfileGeneratedEventAfterProfileGeneration() {
        Long userId = 1L;
        float[] profileVector = new float[]{0.1f, 0.2f};
        PersonalizationProfileDocument profileDocument = PersonalizationProfileDocumentFixture.personalizationProfileDocument(
                userId,
                "Spring과 JPA 기반 백엔드 관심 프로필",
                profileVector,
                List.of("Backend"),
                List.of("Spring", "JPA")
        );
        given(personalizedProfileGenerator.generate(userId)).willReturn(profileDocument);

        personalizationProfileService.generatePersonalizationProfileSync(userId);

        ArgumentCaptor<PersonalizedProfileGeneratedEvent> eventCaptor = ArgumentCaptor.forClass(PersonalizedProfileGeneratedEvent.class);
        verify(personalizedProfileGenerator).generate(userId);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PersonalizedProfileGeneratedEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.profileVector()).containsExactly(0.1f, 0.2f);
        assertThat(event.keyKeywords()).containsExactly("Spring", "JPA");
    }

    @Test
    @DisplayName("개인화 프로필 생성이 실패하면 예외를 전파하고 이벤트를 발행하지 않는다")
    void generatePersonalizationProfileSync_ProfileGenerationFailurePropagatesExceptionAndDoesNotPublishEvent() {
        Long userId = 2L;
        RuntimeException failure = new RuntimeException("profile generation failure");
        given(personalizedProfileGenerator.generate(userId)).willThrow(failure);

        assertThatThrownBy(() -> personalizationProfileService.generatePersonalizationProfileSync(userId))
                .isSameAs(failure);

        verify(personalizedProfileGenerator).generate(userId);
        verifyNoInteractions(eventPublisher);
    }
}
