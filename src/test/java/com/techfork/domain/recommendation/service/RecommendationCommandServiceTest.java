package com.techfork.domain.recommendation.service;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.infrastructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationCommandService 단위 테스트")
class RecommendationCommandServiceTest {

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecommendationCommandService recommendationCommandService;

    @Nested
    @DisplayName("regenerateRecommendations")
    class RegenerateRecommendations {

        @Test
        @DisplayName("userId로 사용자 참조를 조회해 수동 추천 재생성을 요청한다")
        void userIdProvided_GeneratesRecommendationsForUserReference() {
            Long userId = 1L;
            User user = mock(User.class);
            given(userRepository.getReferenceById(userId)).willReturn(user);
            given(recommendationService.generateRecommendationsForUser(user)).willReturn(5);

            recommendationCommandService.regenerateRecommendations(userId);

            verify(userRepository).getReferenceById(userId);
            verify(recommendationService).generateRecommendationsForUser(user);
        }
    }

}
