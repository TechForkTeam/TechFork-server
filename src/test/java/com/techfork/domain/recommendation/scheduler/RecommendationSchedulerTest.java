package com.techfork.domain.recommendation.scheduler;

import com.techfork.domain.recommendation.config.RecommendationProperties;
import com.techfork.domain.recommendation.service.RecommendationService;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.infrastructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationScheduler 단위 테스트")
class RecommendationSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private RecommendationScheduler recommendationScheduler;

    @Test
    @DisplayName("최근 활성 사용자를 조회해 일일 추천 생성을 요청한다")
    void generateDailyRecommendations_GeneratesRecommendationsForActiveUsers() {
        int activeUserHours = 24;
        User user1 = mockUser(1L);
        User user2 = mockUser(2L);
        given(properties.getActiveUserHours()).willReturn(activeUserHours);
        given(userRepository.findActiveUsersSince(any(LocalDateTime.class))).willReturn(List.of(user1, user2));
        given(recommendationService.generateRecommendationsForUser(user1)).willReturn(2);
        given(recommendationService.generateRecommendationsForUser(user2)).willReturn(3);

        LocalDateTime lowerBound = LocalDateTime.now().minusHours(activeUserHours);
        recommendationScheduler.generateDailyRecommendations();
        LocalDateTime upperBound = LocalDateTime.now().minusHours(activeUserHours);

        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).findActiveUsersSince(sinceCaptor.capture());
        assertThat(sinceCaptor.getValue()).isBetween(lowerBound, upperBound);
        verify(recommendationService).generateRecommendationsForUser(user1);
        verify(recommendationService).generateRecommendationsForUser(user2);
    }

    @Test
    @DisplayName("일부 사용자 추천 생성이 실패해도 다음 사용자 처리를 계속한다")
    void generateDailyRecommendations_WhenOneUserFails_ContinuesWithNextUser() {
        User failingUser = mockUser(1L);
        User successfulUser = mockUser(2L);
        given(properties.getActiveUserHours()).willReturn(24);
        given(userRepository.findActiveUsersSince(any(LocalDateTime.class))).willReturn(List.of(failingUser, successfulUser));
        given(recommendationService.generateRecommendationsForUser(failingUser))
                .willThrow(new RuntimeException("recommendation failure"));
        given(recommendationService.generateRecommendationsForUser(successfulUser)).willReturn(3);

        assertThatCode(() -> recommendationScheduler.generateDailyRecommendations())
                .doesNotThrowAnyException();

        verify(recommendationService).generateRecommendationsForUser(failingUser);
        verify(recommendationService).generateRecommendationsForUser(successfulUser);
    }

    @Test
    @DisplayName("일일 추천 스케줄은 매일 7시 KST에 실행된다")
    void generateDailyRecommendations_HasDailySevenAmKstSchedule() throws NoSuchMethodException {
        Scheduled scheduled = RecommendationScheduler.class
                .getDeclaredMethod("generateDailyRecommendations")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 7 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    private User mockUser(Long userId) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        return user;
    }
}
