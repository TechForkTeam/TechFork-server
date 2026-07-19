package com.techfork.personalization.application.query.lookup;

import com.techfork.personalization.infrastructure.PersonalizationProfileDocument;
import com.techfork.personalization.infrastructure.PersonalizationProfileDocumentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalizationProfileLookupServiceTest {

    @Mock
    private PersonalizationProfileDocumentRepository personalizationProfileDocumentRepository;

    @InjectMocks
    private PersonalizationProfileLookupService personalizationProfileLookupService;

    @Nested
    @DisplayName("사용자 개인화 프로필 조회")
    class FindByUserId {

        @Test
        @DisplayName("프로필이 존재하면 추천에 필요한 벡터와 핵심 키워드를 반환한다")
        void profileExists_ReturnsRecommendationProfileData() {
            Long userId = 1L;
            float[] profileVector = new float[]{0.1f, 0.2f};
            List<String> keyKeywords = List.of("Spring", "JPA");
            PersonalizationProfileDocument profileDocument = mock(PersonalizationProfileDocument.class);
            given(profileDocument.getProfileVector()).willReturn(profileVector);
            given(profileDocument.getKeyKeywords()).willReturn(keyKeywords);
            given(personalizationProfileDocumentRepository.findByUserId(userId))
                    .willReturn(Optional.of(profileDocument));

            Optional<PersonalizationProfileLookupItem> result =
                    personalizationProfileLookupService.findByUserId(userId);

            assertThat(result).hasValueSatisfying(profile -> {
                assertThat(profile.profileVector()).isSameAs(profileVector);
                assertThat(profile.keyKeywords()).isEqualTo(keyKeywords);
            });
            verify(personalizationProfileDocumentRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("프로필이 없으면 빈 결과를 반환한다")
        void profileMissing_ReturnsEmpty() {
            Long userId = 999L;
            given(personalizationProfileDocumentRepository.findByUserId(userId)).willReturn(Optional.empty());

            Optional<PersonalizationProfileLookupItem> result =
                    personalizationProfileLookupService.findByUserId(userId);

            assertThat(result).isEmpty();
            verify(personalizationProfileDocumentRepository).findByUserId(userId);
        }
    }
}
