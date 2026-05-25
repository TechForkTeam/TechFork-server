package com.techfork.domain.useraccount.entity;

import com.techfork.domain.useraccount.enums.Role;
import com.techfork.domain.useraccount.enums.SocialType;
import com.techfork.domain.useraccount.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Nested
    @DisplayName("createSocialUser")
    class CreateSocialUser {

        @Test
        @DisplayName("소셜 사용자 생성 시 기본 상태는 PENDING이고 ROLE_USER를 가진다")
        void createsPendingSocialUserWithUserRole() {
            User user = User.createSocialUser(
                    SocialType.KAKAO,
                    "social-id-123",
                    "user@example.com",
                    "https://cdn.example.com/profile.png"
            );

            assertThat(user.getSocialType()).isEqualTo(SocialType.KAKAO);
            assertThat(user.getSocialId()).isEqualTo("social-id-123");
            assertThat(user.getEmail()).isEqualTo("user@example.com");
            assertThat(user.getProfileImage()).isEqualTo("https://cdn.example.com/profile.png");
            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(user.isActive()).isFalse();
            assertThat(user.isWithdrawn()).isFalse();
            assertThat(user.getInterestCategories()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("온보딩 완료 시 계정 정보를 갱신하고 ACTIVE 상태가 된다")
        void activatesUserWhenOnboardingCompletes() {
            User user = User.createSocialUser(SocialType.KAKAO, "social-id-123", "before@example.com", null);

            user.updateUser("테크포크유저", "after@example.com", "백엔드 개발자입니다.");

            assertThat(user.getNickName()).isEqualTo("테크포크유저");
            assertThat(user.getEmail()).isEqualTo("after@example.com");
            assertThat(user.getDescription()).isEqualTo("백엔드 개발자입니다.");
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isActive()).isTrue();
            assertThat(user.isWithdrawn()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("계정 프로필 수정 시 전달된 필드만 변경한다")
        void updatesOnlyProvidedProfileFields() {
            User user = activeUser();

            user.updateProfile("새닉네임", null);

            assertThat(user.getNickName()).isEqualTo("새닉네임");
            assertThat(user.getDescription()).isEqualTo("기존 자기소개");

            user.updateProfile(null, "새 자기소개");

            assertThat(user.getNickName()).isEqualTo("새닉네임");
            assertThat(user.getDescription()).isEqualTo("새 자기소개");
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("탈퇴 시 개인정보를 null 처리하고 WITHDRAWN 상태가 된다")
        void anonymizesPersonalDataAndMarksWithdrawn() {
            User user = activeUser();
            String originalSocialId = user.getSocialId();
            SocialType originalSocialType = user.getSocialType();
            Role originalRole = user.getRole();

            user.withdraw();

            assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(user.isWithdrawn()).isTrue();
            assertThat(user.isActive()).isFalse();
            assertThat(user.getNickName()).isNull();
            assertThat(user.getEmail()).isNull();
            assertThat(user.getProfileImage()).isNull();
            assertThat(user.getDescription()).isNull();
            assertThat(user.getSocialId()).isEqualTo(originalSocialId);
            assertThat(user.getSocialType()).isEqualTo(originalSocialType);
            assertThat(user.getRole()).isEqualTo(originalRole);
        }
    }

    @Nested
    @DisplayName("reactivate")
    class Reactivate {

        @Test
        @DisplayName("재활성화 시 이메일과 프로필 이미지를 복구하고 PENDING 상태가 된다")
        void reactivatesUserAsPendingWithRecoveredIdentityData() {
            User user = activeUser();
            user.withdraw();

            user.reactivate("reactivated@example.com", "https://cdn.example.com/reactivated.png");

            assertThat(user.getEmail()).isEqualTo("reactivated@example.com");
            assertThat(user.getProfileImage()).isEqualTo("https://cdn.example.com/reactivated.png");
            assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(user.isActive()).isFalse();
            assertThat(user.isWithdrawn()).isFalse();
            assertThat(user.getNickName()).isNull();
            assertThat(user.getDescription()).isNull();
        }
    }

    private User activeUser() {
        User user = User.createSocialUser(
                SocialType.KAKAO,
                "social-id-123",
                "user@example.com",
                "https://cdn.example.com/profile.png"
        );
        user.updateUser("기존닉네임", "user@example.com", "기존 자기소개");
        return user;
    }
}
