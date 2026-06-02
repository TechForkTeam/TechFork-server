package com.techfork.useraccount.application.command;

import com.techfork.useraccount.application.command.input.UpdateUserInterestsCommand;
import com.techfork.useraccount.application.command.input.UserInterestCommand;
import com.techfork.domain.personalization.service.PersonalizationProfileService;
import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.UserInterestKeyword;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterestCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonalizationProfileService personalizationProfileService;

    @InjectMocks
    private InterestCommandService interestCommandService;

    @Test
    @DisplayName("관심사 저장 - 요청을 도메인 선택값으로 변환하고 개인화 프로필 생성을 트리거한다")
    void saveUserInterests_ConvertsCommandAndTriggersProfileGeneration() {
        Long userId = 1L;
        User user = createUserWithId(userId);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder()
                        .category("BACKEND")
                        .keywords(List.of("JAVA", "SPRING"))
                        .build()
        );

        interestCommandService.saveUserInterests(user, interests);

        UserInterestCategory category = user.getInterestCategories().get(0);
        assertThat(user.getInterestCategories()).hasSize(1);
        assertThat(category.getCategory()).isEqualTo(EInterestCategory.BACKEND);
        assertThat(category.getKeywords())
                .extracting(UserInterestKeyword::getKeyword)
                .containsExactly(EInterestKeyword.JAVA, EInterestKeyword.SPRING);
        verify(personalizationProfileService).generatePersonalizationProfile(userId);
    }

    @Test
    @DisplayName("관심사 저장 - 키워드가 없는 요청도 카테고리만 저장하고 개인화 프로필 생성을 트리거한다")
    void saveUserInterests_CategoryOnly_TriggersProfileGeneration() {
        Long userId = 1L;
        User user = createUserWithId(userId);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder()
                        .category("AI_ML")
                        .keywords(null)
                        .build()
        );

        interestCommandService.saveUserInterests(user, interests);

        UserInterestCategory category = user.getInterestCategories().get(0);
        assertThat(user.getInterestCategories()).hasSize(1);
        assertThat(category.getCategory()).isEqualTo(EInterestCategory.AI_ML);
        assertThat(category.getKeywords()).isEmpty();
        verify(personalizationProfileService).generatePersonalizationProfile(userId);
    }

    @Test
    @DisplayName("관심사 저장 - 잘못된 카테고리와 키워드 조합이면 개인화 프로필 생성을 트리거하지 않는다")
    void saveUserInterests_InvalidKeywordCategory_SkipsProfileGeneration() {
        User user = createUserWithId(1L);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("REACT")).build()
        );

        assertThatThrownBy(() -> interestCommandService.saveUserInterests(user, interests))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.INVALID_INTEREST_KEYWORD);

        verify(personalizationProfileService, never()).generatePersonalizationProfile(any());
    }

    @Test
    @DisplayName("관심사 업데이트 - 사용자를 조회해 관심사를 교체하고 개인화 프로필 생성을 트리거한다")
    void updateUserInterests_Success() {
        Long userId = 1L;
        User user = createUserWithId(userId);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("AI_ML").keywords(List.of("TENSORFLOW", "PYTORCH")).build()
        );
        given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.of(user));

        interestCommandService.updateUserInterests(new UpdateUserInterestsCommand(userId, interests));

        assertThat(user.getInterestCategories()).hasSize(1);
        verify(userRepository).findByIdWithInterestCategories(userId);
        verify(personalizationProfileService).generatePersonalizationProfile(userId);
    }

    @Test
    @DisplayName("관심사 업데이트 - 사용자가 존재하지 않으면 예외가 발생하고 개인화 프로필 생성을 트리거하지 않는다")
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

    private User createUserWithId(Long userId) {
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
