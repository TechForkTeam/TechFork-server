package com.techfork.personalization.scheduler;

import com.techfork.personalization.application.PersonalizationProfileService;
import com.techfork.useraccount.application.query.lookup.UserLookupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalizationProfileSchedulerTest {

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private PersonalizationProfileService personalizationProfileService;

    @InjectMocks
    private PersonalizationProfileScheduler personalizationProfileScheduler;

    @Nested
    @DisplayName("regenerateActiveUserProfiles")
    class RegenerateActiveUserProfiles {

        @Test
        @DisplayName("최근 활성 사용자 ID를 lookup으로 조회해 개인화 프로필 생성을 요청한다")
        void activeUserIdsReturned_GeneratesProfilesForEachUser() {
            given(userLookupService.getActiveUserIdsSince(any(LocalDateTime.class)))
                    .willReturn(List.of(1L, 2L));

            personalizationProfileScheduler.regenerateActiveUserProfiles();

            ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(userLookupService).getActiveUserIdsSince(sinceCaptor.capture());
            assertThat(sinceCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
            verify(personalizationProfileService).generatePersonalizationProfile(1L);
            verify(personalizationProfileService).generatePersonalizationProfile(2L);
        }
    }

}
