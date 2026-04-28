package com.techfork.domain.user.service;

import com.techfork.domain.user.dto.SaveInterestRequest;
import com.techfork.domain.user.dto.UserInterestDto;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.entity.UserInterestCategory;
import com.techfork.domain.user.enums.SocialType;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * InterestCommandService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class InterestCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonalizationProfileService personalizationProfileService;

    @InjectMocks
    private InterestCommandService interestCommandService;

    @Test
    @DisplayName("관심사 저장 - 정상 케이스")
    void saveUserInterests_Success() {
        // Given
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("BACKEND")
                        .keywords(List.of("JAVA", "SPRING"))
                        .build()
        );

        SaveInterestRequest request = new SaveInterestRequest(interests);

        // When
        interestCommandService.saveUserInterests(user, request);

        // Then
        assertThat(user.getInterestCategories()).hasSize(1);
        assertThat(user.getInterestCategories().get(0).getKeywords()).hasSize(2);

        verify(personalizationProfileService, times(1)).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 저장 - 키워드 없이 카테고리만 저장")
    void saveUserInterests_CategoryOnly_Success() {
        // Given
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("AI_ML")
                        .keywords(null)
                        .build()
        );

        SaveInterestRequest request = new SaveInterestRequest(interests);

        // When
        interestCommandService.saveUserInterests(user, request);

        // Then
        assertThat(user.getInterestCategories()).hasSize(1);
        assertThat(user.getInterestCategories().get(0).getKeywords()).isEmpty();

        verify(personalizationProfileService, times(1)).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 저장 - 여러 카테고리와 키워드")
    void saveUserInterests_MultipleCategories_Success() {
        // Given
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("BACKEND")
                        .keywords(List.of("JAVA", "SPRING", "PYTHON"))
                        .build(),
                UserInterestDto.builder()
                        .category("DATABASE")
                        .keywords(List.of("MYSQL", "REDIS"))
                        .build(),
                UserInterestDto.builder()
                        .category("DEVOPS")
                        .keywords(List.of("DOCKER", "KUBERNETES", "CI_CD"))
                        .build()
        );

        SaveInterestRequest request = new SaveInterestRequest(interests);

        // When
        interestCommandService.saveUserInterests(user, request);

        // Then
        assertThat(user.getInterestCategories()).hasSize(3);
        assertThat(user.getInterestCategories().get(0).getKeywords()).hasSize(3);
        assertThat(user.getInterestCategories().get(1).getKeywords()).hasSize(2);
        assertThat(user.getInterestCategories().get(2).getKeywords()).hasSize(3);

        verify(personalizationProfileService, times(1)).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 저장 - 기존 관심사를 clear하고 새로 저장")
    void saveUserInterests_ClearExistingInterests_Success() {
        // Given
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        // 기존 관심사 추가
        user.getInterestCategories().add(mock(UserInterestCategory.class));
        user.getInterestCategories().add(mock(UserInterestCategory.class));
        assertThat(user.getInterestCategories()).hasSize(2);

        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("FRONTEND")
                        .keywords(List.of("REACT"))
                        .build()
        );

        SaveInterestRequest request = new SaveInterestRequest(interests);

        // When
        interestCommandService.saveUserInterests(user, request);

        // Then
        assertThat(user.getInterestCategories()).hasSize(1);

        verify(personalizationProfileService, times(1)).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 저장 - 잘못된 카테고리와 키워드 조합이면 예외 발생")
    void saveUserInterests_InvalidKeywordCategory_ThrowsException() {
        // Given
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        // BACKEND 카테고리에 FRONTEND 키워드를 넣으려고 시도
        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("BACKEND")
                        .keywords(List.of("REACT")) // REACT는 FRONTEND 키워드
                        .build()
        );

        SaveInterestRequest request = new SaveInterestRequest(interests);

        // When & Then
        assertThatThrownBy(() -> interestCommandService.saveUserInterests(user, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.INVALID_INTEREST_KEYWORD);

        verify(personalizationProfileService, never()).generatePersonalizationProfile(any());
    }

    @Test
    @DisplayName("관심사 업데이트 - 정상 케이스")
    void updateUserInterests_Success() {
        // Given
        Long userId = 1L;
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        ReflectionTestUtils.setField(mockUser, "id", userId);

        List<UserInterestDto> interests = List.of(
                UserInterestDto.builder()
                        .category("AI_ML")
                        .keywords(List.of("TENSORFLOW", "PYTORCH"))
                        .build()
        );

        SaveInterestRequest request = new SaveInterestRequest(interests);

        given(userRepository.findByIdWithInterestCategories(userId))
                .willReturn(Optional.of(mockUser));

        // When
        interestCommandService.updateUserInterests(userId, request);

        // Then
        assertThat(mockUser.getInterestCategories()).hasSize(1);

        verify(userRepository, times(1)).findByIdWithInterestCategories(userId);
        verify(personalizationProfileService, times(1)).generatePersonalizationProfile(userId);
    }

    @Test
    @DisplayName("관심사 업데이트 - 사용자가 존재하지 않으면 예외 발생")
    void updateUserInterests_UserNotFound_ThrowsException() {
        // Given
        Long userId = 999L;
        SaveInterestRequest request = new SaveInterestRequest(
                List.of(
                        UserInterestDto.builder()
                                .category("BACKEND")
                                .keywords(List.of("JAVA"))
                                .build()
                )
        );

        given(userRepository.findByIdWithInterestCategories(userId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> interestCommandService.updateUserInterests(userId, request))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(personalizationProfileService, never()).generatePersonalizationProfile(any());
    }
}
