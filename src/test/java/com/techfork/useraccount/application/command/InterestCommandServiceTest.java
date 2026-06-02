package com.techfork.useraccount.application.command;

import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.application.command.input.UpdateUserInterestsCommand;
import com.techfork.useraccount.application.command.input.UserInterestCommand;
import com.techfork.useraccount.application.event.UserInterestsChangedEvent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterestCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InterestCommandService interestCommandService;

    @Test
    @DisplayName("관심사 저장 - 요청을 도메인 선택값으로 변환한다")
    void saveUserInterests_ConvertsCommand() {
        User user = createUserWithId(1L);
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
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("관심사 저장 - 키워드가 없는 요청도 카테고리만 저장한다")
    void saveUserInterests_CategoryOnly() {
        User user = createUserWithId(1L);
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
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("관심사 저장 - 잘못된 카테고리와 키워드 조합이면 이벤트를 발행하지 않는다")
    void saveUserInterests_InvalidKeywordCategory_SkipsEventPublishing() {
        User user = createUserWithId(1L);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("REACT")).build()
        );

        assertThatThrownBy(() -> interestCommandService.saveUserInterests(user, interests))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.INVALID_INTEREST_KEYWORD);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("관심사 저장 - 존재하지 않는 카테고리면 이벤트를 발행하지 않는다")
    void saveUserInterests_UnknownCategory_SkipsEventPublishing() {
        User user = createUserWithId(1L);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("UNKNOWN").keywords(List.of("JAVA")).build()
        );

        assertThatThrownBy(() -> interestCommandService.saveUserInterests(user, interests))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.INVALID_INTEREST_CATEGORY);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("관심사 저장 - 존재하지 않는 키워드면 이벤트를 발행하지 않는다")
    void saveUserInterests_UnknownKeyword_SkipsEventPublishing() {
        User user = createUserWithId(1L);
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("UNKNOWN")).build()
        );

        assertThatThrownBy(() -> interestCommandService.saveUserInterests(user, interests))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.INVALID_INTEREST_KEYWORD);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("관심사 업데이트 - 사용자를 조회해 관심사를 교체하고 이벤트를 발행한다")
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
        verifyPublishedEvent(userId);
    }

    @Test
    @DisplayName("관심사 업데이트 - 사용자가 존재하지 않으면 예외가 발생하고 이벤트를 발행하지 않는다")
    void updateUserInterests_UserNotFound_ThrowsException() {
        Long userId = 999L;
        List<UserInterestCommand> interests = List.of(
                UserInterestCommand.builder().category("BACKEND").keywords(List.of("JAVA")).build()
        );
        given(userRepository.findByIdWithInterestCategories(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> interestCommandService.updateUserInterests(new UpdateUserInterestsCommand(userId, interests)))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(eventPublisher, never()).publishEvent(any());
    }

    private User createUserWithId(Long userId) {
        User user = User.createSocialUser(SocialType.KAKAO, "testSocialId", "test@example.com", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private void verifyPublishedEvent(Long expectedUserId) {
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue()).isInstanceOf(UserInterestsChangedEvent.class);
        UserInterestsChangedEvent event = (UserInterestsChangedEvent) eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(expectedUserId);
    }
}
