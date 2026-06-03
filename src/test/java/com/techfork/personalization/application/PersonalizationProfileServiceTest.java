package com.techfork.personalization.application;

import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.personalization.application.generation.PersonalizedProfileGenerator;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PersonalizationProfileServiceTest {

    @Mock
    private PersonalizedProfileGenerator personalizedProfileGenerator;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private PersonalizationProfileService personalizationProfileService;

    @Test
    @DisplayName("개인화 프로필 생성을 요청한 뒤 추천을 생성한다")
    void generatePersonalizationProfileSync_GeneratesProfileAndRecommendations() {
        Long userId = 1L;
        User user = createUser(userId);
        given(userLookupService.getUserOrThrow(userId)).willReturn(user);
        given(recommendationService.generateRecommendationsForUser(user)).willReturn(5);

        personalizationProfileService.generatePersonalizationProfileSync(userId);

        verify(personalizedProfileGenerator).generate(userId);
        verify(userLookupService).getUserOrThrow(userId);
        verify(recommendationService).generateRecommendationsForUser(user);
    }

    @Test
    @DisplayName("개인화 프로필 생성이 실패하면 예외를 전파하고 추천을 시도하지 않는다")
    void generatePersonalizationProfileSync_ProfileGenerationFailurePropagatesException() {
        Long userId = 2L;
        RuntimeException failure = new RuntimeException("profile generation failure");
        given(personalizedProfileGenerator.generate(userId)).willThrow(failure);

        assertThatThrownBy(() -> personalizationProfileService.generatePersonalizationProfileSync(userId))
                .isSameAs(failure);

        verify(personalizedProfileGenerator).generate(userId);
        verifyNoInteractions(userLookupService, recommendationService);
    }

    @Test
    @DisplayName("추천 생성이 실패해도 개인화 프로필 저장은 유지된다")
    void generatePersonalizationProfileSync_RecommendationFailureDoesNotBreakProfileSave() {
        Long userId = 3L;
        User user = createUser(userId);
        given(userLookupService.getUserOrThrow(userId)).willReturn(user);
        given(recommendationService.generateRecommendationsForUser(user))
                .willThrow(new RuntimeException("recommendation failure"));

        assertThatCode(() -> personalizationProfileService.generatePersonalizationProfileSync(userId))
                .doesNotThrowAnyException();

        verify(personalizedProfileGenerator).generate(userId);
        verify(recommendationService).generateRecommendationsForUser(user);
    }

    private User createUser(Long userId) {
        User user = User.createSocialUser(SocialType.KAKAO, "social-" + userId, "user" + userId + "@example.com", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
