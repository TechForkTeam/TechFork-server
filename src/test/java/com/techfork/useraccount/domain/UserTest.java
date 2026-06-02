package com.techfork.useraccount.domain;

import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.enums.Role;
import com.techfork.useraccount.domain.enums.SocialType;
import com.techfork.useraccount.domain.enums.UserStatus;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.domain.vo.UserInterestSelection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("replaceInterests")
    class ReplaceInterests {

        @Test
        @DisplayName("관심사 교체 시 카테고리와 키워드를 함께 조립한다")
        void replacesInterestsWithCategoriesAndKeywords() {
            User user = activeUser();

            user.replaceInterests(List.of(
                    new UserInterestSelection(EInterestCategory.BACKEND, List.of(EInterestKeyword.JAVA, EInterestKeyword.SPRING)),
                    new UserInterestSelection(EInterestCategory.DATABASE, List.of(EInterestKeyword.MYSQL, EInterestKeyword.REDIS))
            ));

            assertThat(user.getInterestCategories()).hasSize(2);
            UserInterestCategory backend = findCategory(user, EInterestCategory.BACKEND);
            assertThat(backend.getUser()).isSameAs(user);
            assertThat(backend.getKeywords())
                    .extracting(UserInterestKeyword::getKeyword)
                    .containsExactly(EInterestKeyword.JAVA, EInterestKeyword.SPRING);
            assertThat(backend.getKeywords())
                    .allSatisfy(keyword -> assertThat(keyword.getUserInterestCategory()).isSameAs(backend));

            UserInterestCategory database = findCategory(user, EInterestCategory.DATABASE);
            assertThat(database.getKeywords())
                    .extracting(UserInterestKeyword::getKeyword)
                    .containsExactly(EInterestKeyword.MYSQL, EInterestKeyword.REDIS);
        }

        @Test
        @DisplayName("키워드가 없으면 카테고리만 교체한다")
        void replacesInterestsWithCategoryOnly() {
            User user = activeUser();

            user.replaceInterests(List.of(
                    new UserInterestSelection(EInterestCategory.AI_ML, null)
            ));

            assertThat(user.getInterestCategories()).hasSize(1);
            UserInterestCategory aiMl = findCategory(user, EInterestCategory.AI_ML);
            assertThat(aiMl.getKeywords()).isEmpty();
        }

        @Test
        @DisplayName("기존 관심사를 제거하고 새 관심사로 교체한다")
        void clearsExistingInterestsBeforeReplacing() {
            User user = activeUser();
            user.replaceInterests(List.of(
                    new UserInterestSelection(EInterestCategory.BACKEND, List.of(EInterestKeyword.JAVA, EInterestKeyword.SPRING))
            ));

            user.replaceInterests(List.of(
                    new UserInterestSelection(EInterestCategory.FRONTEND, List.of(EInterestKeyword.REACT))
            ));

            assertThat(user.getInterestCategories())
                    .extracting(UserInterestCategory::getCategory)
                    .containsExactly(EInterestCategory.FRONTEND);
            assertThat(findCategory(user, EInterestCategory.FRONTEND).getKeywords())
                    .extracting(UserInterestKeyword::getKeyword)
                    .containsExactly(EInterestKeyword.REACT);
        }

        @Test
        @DisplayName("키워드는 선택된 카테고리에 속해야 한다")
        void rejectsKeywordThatDoesNotBelongToCategory() {
            User user = activeUser();
            user.replaceInterests(List.of(
                    new UserInterestSelection(EInterestCategory.BACKEND, List.of(EInterestKeyword.JAVA))
            ));

            assertThatThrownBy(() -> user.replaceInterests(List.of(
                    new UserInterestSelection(EInterestCategory.BACKEND, List.of(EInterestKeyword.REACT))
            )))
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue("code", UserErrorCode.INVALID_INTEREST_KEYWORD);

            assertThat(user.getInterestCategories())
                    .extracting(UserInterestCategory::getCategory)
                    .containsExactly(EInterestCategory.BACKEND);
            assertThat(findCategory(user, EInterestCategory.BACKEND).getKeywords())
                    .extracting(UserInterestKeyword::getKeyword)
                    .containsExactly(EInterestKeyword.JAVA);
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

    private UserInterestCategory findCategory(User user, EInterestCategory category) {
        return user.getInterestCategories().stream()
                .filter(interestCategory -> interestCategory.getCategory() == category)
                .findFirst()
                .orElseThrow();
    }
}
