package com.techfork.useraccount.application.auth;

import com.techfork.useraccount.application.event.UserReactivatedEvent;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.useraccount.fixture.UserFixture;
import com.techfork.useraccount.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAuthAccountServiceTest {

    private static final Long USER_ID = 1L;
    private static final String SOCIAL_ID = "social-id-123";
    private static final String EMAIL = "user@example.com";
    private static final String PROFILE_IMAGE = "https://cdn.example.com/profile.png";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserAuthAccountService userAuthAccountService;

    @Nested
    @DisplayName("findAuthProfileById")
    class FindAuthProfileById {

        @Test
        @DisplayName("존재하는 사용자의 인증 프로필 스냅샷을 반환한다")
        void returnsAuthProfileSnapshot() {
            User user = UserFixture.socialUserWithId(USER_ID, SocialType.KAKAO, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            Optional<UserAuthProfile> result = userAuthAccountService.findAuthProfileById(USER_ID);

            assertThat(result).hasValueSatisfying(profile -> {
                assertThat(profile.id()).isEqualTo(USER_ID);
                assertThat(profile.role()).isEqualTo(Role.USER);
                assertThat(profile.status()).isEqualTo(UserStatus.PENDING);
                assertThat(profile.email()).isEqualTo(EMAIL);
                assertThat(profile.active()).isFalse();
            });
            verify(userRepository).findById(USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 사용자는 빈 결과를 반환한다")
        void returnsEmptyWhenUserNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            Optional<UserAuthProfile> result = userAuthAccountService.findAuthProfileById(USER_ID);

            assertThat(result).isEmpty();
            verify(userRepository).findById(USER_ID);
        }
    }

    @Nested
    @DisplayName("getOrCreateSocialAuthProfile")
    class GetOrCreateSocialAuthProfile {

        @Test
        @DisplayName("신규 소셜 사용자를 생성하고 인증 프로필을 반환한다")
        void createsNewSocialUserAndReturnsAuthProfile() {
            User savedUser = UserFixture.socialUserWithId(USER_ID, SocialType.KAKAO, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, SOCIAL_ID))
                    .willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            UserAuthProfile result = userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.KAKAO,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.role()).isEqualTo(Role.USER);
            assertThat(result.status()).isEqualTo(UserStatus.PENDING);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.active()).isFalse();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User newUser = userCaptor.getValue();
            assertThat(newUser.getSocialType()).isEqualTo(SocialType.KAKAO);
            assertThat(newUser.getSocialId()).isEqualTo(SOCIAL_ID);
            assertThat(newUser.getEmail()).isEqualTo(EMAIL);
            assertThat(newUser.getProfileImage()).isEqualTo(PROFILE_IMAGE);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("기존 소셜 사용자를 재사용하고 새로 저장하지 않는다")
        void reusesExistingSocialUserWithoutSaving() {
            User existingUser = UserFixture.activeUserWithId(USER_ID, SocialType.KAKAO, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, SOCIAL_ID))
                    .willReturn(Optional.of(existingUser));

            UserAuthProfile result = userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.KAKAO,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.active()).isTrue();
            verify(userRepository, never()).save(any(User.class));
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("탈퇴 소셜 사용자를 재활성화하고 PENDING 인증 프로필을 반환한다")
        void reactivatesWithdrawnSocialUserAndReturnsPendingAuthProfile() {
            User withdrawnUser = UserFixture.activeUserWithId(USER_ID, SocialType.KAKAO, SOCIAL_ID, "old@example.com", "old.png");
            withdrawnUser.withdraw();
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, SOCIAL_ID))
                    .willReturn(Optional.of(withdrawnUser));

            UserAuthProfile result = userAuthAccountService.getOrCreateSocialAuthProfile(
                    SocialType.KAKAO,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.status()).isEqualTo(UserStatus.PENDING);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.active()).isFalse();
            assertThat(withdrawnUser.getEmail()).isEqualTo(EMAIL);
            assertThat(withdrawnUser.getProfileImage()).isEqualTo(PROFILE_IMAGE);
            assertThat(withdrawnUser.getStatus()).isEqualTo(UserStatus.PENDING);
            verify(userRepository, never()).save(any(User.class));
            ArgumentCaptor<UserReactivatedEvent> eventCaptor = ArgumentCaptor.forClass(UserReactivatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(USER_ID);
        }
    }

}
