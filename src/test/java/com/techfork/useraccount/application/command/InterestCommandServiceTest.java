package com.techfork.useraccount.application.command;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.domain.personalization.service.PersonalizationProfileService;
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
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder()
                        .category("BACKEND")
                        .keywords(List.of("JAVA", "SPRING"))
                        .build()
        );

        interestCommandService.saveUserInterests(user, interests);

        assertThat(user.getInterestCategories()).hasSize(1);
        assertThat(user.getInterestCategories().get(0).getKeywords()).hasSize(2);
        verify(personalizationProfileService).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 저장 - 키워드 없이 카테고리만 저장")
    void saveUserInterests_CategoryOnly_Success() {
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder()
                        .category("AI_ML")
                        .keywords(null)
                        .build()
        );

        interestCommandService.saveUserInterests(user, interests);

        assertThat(user.getInterestCategories()).hasSize(1);
        assertThat(user.getInterestCategories().get(0).getKeywords()).isEmpty();
        verify(personalizationProfileService).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 저장 - 여러 카테고리와 키워드")
    void saveUserInterests_MultipleCategories_Success() {
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("JAVA", "SPRING", "PYTHON")).build(),
                UserInterestCommand.builder().category("DATABASE").keywords(List.of("MYSQL", "REDIS")).build(),
                UserInterestCommand.builder().category("DEVOPS").keywords(List.of("DOCKER", "KUBERNETES", "CI_CD")).build()
        );

        interestCommandService.saveUserInterests(user, interests);

        assertThat(user.getInterestCategories()).hasSize(3);
        assertThat(user.getInterestCategories().get(0).getKeywords()).hasSize(3);
        assertThat(user.getInterestCategories().get(1).getKeywords()).hasSize(2);
        assertThat(user.getInterestCategories().get(2).getKeywords()).hasSize(3);
        verify(personalizationProfileService).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 저장 - 기존 관심사를 clear하고 새로 저장")
    void saveUserInterests_ClearExistingInterests_Success() {
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        user.getInterestCategories().add(mock(UserInterestCategory.class));
        user.getInterestCategories().add(mock(UserInterestCategory.class));
        assertThat(user.getInterestCategories()).hasSize(2);

        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("FRONTEND").keywords(List.of("REACT")).build()
        );

        interestCommandService.saveUserInterests(user, interests);

        assertThat(user.getInterestCategories()).hasSize(1);
        verify(personalizationProfileService).generatePersonalizationProfile(user.getId());
    }

    @Test
    @DisplayName("관심사 저장 - 잘못된 카테고리와 키워드 조합이면 예외 발생")
    void saveUserInterests_InvalidKeywordCategory_ThrowsException() {
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);

        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("REACT")).build()
        );

        assertThatThrownBy(() -> interestCommandService.saveUserInterests(user, interests))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.INVALID_INTEREST_KEYWORD);

        verify(personalizationProfileService, never()).generatePersonalizationProfile(any());
    }

    @Test
    @DisplayName("관심사 업데이트 - 정상 케이스")
    void updateUserInterests_Success() {
        Long userId = 1L;
        User mockUser = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        ReflectionTestUtils.setField(mockUser, "id", userId);

        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("AI_ML").keywords(List.of("TENSORFLOW", "PYTORCH")).build()
        );

        given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.of(mockUser));

        interestCommandService.updateUserInterests(new UpdateUserInterestsCommand(userId, interests));

        assertThat(mockUser.getInterestCategories()).hasSize(1);
        verify(userRepository).findByIdWithInterestCategories(userId);
        verify(personalizationProfileService).generatePersonalizationProfile(userId);
    }

    @Test
    @DisplayName("관심사 업데이트 - 사용자가 존재하지 않으면 예외 발생")
    void updateUserInterests_UserNotFound_ThrowsException() {
        Long userId = 999L;
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("JAVA")).build()
        );

        given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> interestCommandService.updateUserInterests(new UpdateUserInterestsCommand(userId, interests)))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(personalizationProfileService, never()).generatePersonalizationProfile(any());
    }
}
