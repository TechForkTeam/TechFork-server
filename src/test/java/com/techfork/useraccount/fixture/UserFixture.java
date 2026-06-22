package com.techfork.useraccount.fixture;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.UserInterestKeyword;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.enums.SocialType;
import org.springframework.test.util.ReflectionTestUtils;

public final class UserFixture {

    public static final SocialType DEFAULT_SOCIAL_TYPE = SocialType.KAKAO;
    public static final String DEFAULT_SOCIAL_ID = "testSocialId";
    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_PROFILE_IMAGE = "profile.jpg";
    public static final String DEFAULT_NICKNAME = "테스트유저";
    public static final String DEFAULT_DESCRIPTION = "백엔드 개발자입니다.";

    private UserFixture() {
    }

    public static User socialUser() {
        return socialUser(DEFAULT_SOCIAL_ID, DEFAULT_EMAIL, DEFAULT_PROFILE_IMAGE);
    }

    public static User socialUser(String socialId, String email, String profileImage) {
        return socialUser(DEFAULT_SOCIAL_TYPE, socialId, email, profileImage);
    }

    public static User socialUser(SocialType socialType, String socialId, String email, String profileImage) {
        return User.createSocialUser(socialType, socialId, email, profileImage);
    }

    public static User socialUserWithId(Long id) {
        return socialUserWithId(id, DEFAULT_SOCIAL_ID, DEFAULT_EMAIL, null);
    }

    public static User socialUserWithId(Long id, String socialId, String email, String profileImage) {
        return socialUserWithId(id, DEFAULT_SOCIAL_TYPE, socialId, email, profileImage);
    }

    public static User socialUserWithId(Long id, SocialType socialType, String socialId, String email, String profileImage) {
        return withId(socialUser(socialType, socialId, email, profileImage), id);
    }

    public static User activeUser() {
        return activeUser(DEFAULT_SOCIAL_ID, DEFAULT_EMAIL, DEFAULT_PROFILE_IMAGE);
    }

    public static User activeUser(String socialId, String email, String profileImage) {
        User user = socialUser(socialId, email, profileImage);
        user.updateUser(DEFAULT_NICKNAME, email, DEFAULT_DESCRIPTION);
        return user;
    }

    public static User activeUserWithId(Long id) {
        return activeUserWithId(id, DEFAULT_SOCIAL_ID, DEFAULT_EMAIL, DEFAULT_PROFILE_IMAGE);
    }

    public static User activeUserWithId(Long id, String socialId, String email, String profileImage) {
        return activeUserWithId(id, DEFAULT_SOCIAL_TYPE, socialId, email, profileImage);
    }

    public static User activeUserWithId(Long id, SocialType socialType, String socialId, String email, String profileImage) {
        User user = socialUserWithId(id, socialType, socialId, email, profileImage);
        user.updateUser(DEFAULT_NICKNAME, email, DEFAULT_DESCRIPTION);
        return user;
    }

    public static User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static UserInterestCategory interestCategory(User user, EInterestCategory category, EInterestKeyword... keywords) {
        UserInterestCategory userInterestCategory = UserInterestCategory.create(user, category);
        for (EInterestKeyword keyword : keywords) {
            userInterestCategory.addKeyword(UserInterestKeyword.create(userInterestCategory, keyword));
        }
        return userInterestCategory;
    }
}
