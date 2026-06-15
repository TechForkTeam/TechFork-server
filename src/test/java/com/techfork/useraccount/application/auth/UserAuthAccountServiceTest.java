package com.techfork.useraccount.application.auth;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

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

    @InjectMocks
    private UserAuthAccountService userAuthAccountService;

    @Nested
    @DisplayName("findAuthProfileById")
    class FindAuthProfileById {

        @Test
        @DisplayName("존재하는 사용자의 인증 프로필 스냅샷을 반환한다")
        void returnsAuthProfileSnapshot() {
            User user = pendingUser(USER_ID, SocialType.KAKAO, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
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
            User savedUser = pendingUser(USER_ID, SocialType.KAKAO, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
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
        }

        @Test
        @DisplayName("기존 소셜 사용자를 재사용하고 새로 저장하지 않는다")
        void reusesExistingSocialUserWithoutSaving() {
            User existingUser = activeUser(USER_ID, SocialType.KAKAO, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
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
        }

        @Test
        @DisplayName("탈퇴 소셜 사용자는 재활성화하지 않고 현재 인증 상태만 반환한다")
        void doesNotReactivateWithdrawnSocialUser() {
            User withdrawnUser = activeUser(USER_ID, SocialType.KAKAO, SOCIAL_ID, "old@example.com", "old.png");
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
            assertThat(result.status()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(result.email()).isNull();
            assertThat(result.active()).isFalse();
            assertThat(withdrawnUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("getOrCreateReactivatedSocialAuthProfile")
    class GetOrCreateReactivatedSocialAuthProfile {

        @Test
        @DisplayName("탈퇴 소셜 사용자를 재활성화하고 PENDING 인증 프로필을 반환한다")
        void reactivatesWithdrawnSocialUserAndReturnsPendingAuthProfile() {
            User withdrawnUser = activeUser(USER_ID, SocialType.APPLE, SOCIAL_ID, "old@example.com", "old.png");
            withdrawnUser.withdraw();
            given(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, SOCIAL_ID))
                    .willReturn(Optional.of(withdrawnUser));

            UserAuthProfile result = userAuthAccountService.getOrCreateReactivatedSocialAuthProfile(
                    SocialType.APPLE,
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
        }

        @Test
        @DisplayName("신규 소셜 사용자를 생성하고 인증 프로필을 반환한다")
        void createsNewSocialUserAndReturnsAuthProfile() {
            User savedUser = pendingUser(USER_ID, SocialType.APPLE, SOCIAL_ID, EMAIL, PROFILE_IMAGE);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, SOCIAL_ID))
                    .willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            UserAuthProfile result = userAuthAccountService.getOrCreateReactivatedSocialAuthProfile(
                    SocialType.APPLE,
                    SOCIAL_ID,
                    EMAIL,
                    PROFILE_IMAGE
            );

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.status()).isEqualTo(UserStatus.PENDING);
            assertThat(result.email()).isEqualTo(EMAIL);
            verify(userRepository).save(any(User.class));
        }
    }

    private User pendingUser(Long id, SocialType socialType, String socialId, String email, String profileImage) {
        User user = User.createSocialUser(socialType, socialId, email, profileImage);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User activeUser(Long id, SocialType socialType, String socialId, String email, String profileImage) {
        User user = pendingUser(id, socialType, socialId, email, profileImage);
        user.updateUser("테크포크유저", email, "백엔드 개발자입니다.");
        return user;
    }
}
