package com.techfork.useraccount.fixture;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;
import org.springframework.test.util.ReflectionTestUtils;

public final class UserFixture {

    private static final SocialType DEFAULT_SOCIAL_TYPE = SocialType.KAKAO;
    private static final String DEFAULT_SOCIAL_ID = "testSocialId";
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_PROFILE_IMAGE = "profile.jpg";
    private static final String DEFAULT_NICKNAME = "테스트유저";
    private static final String DEFAULT_DESCRIPTION = "백엔드 개발자입니다.";

    private UserFixture() {
    }

    public static User socialUser(String socialId, String email) {
        return socialUser(socialId, email, DEFAULT_PROFILE_IMAGE);
    }

    public static User socialUser(String socialId, String email, String profileImage) {
        return socialUser(DEFAULT_SOCIAL_TYPE, socialId, email, profileImage);
    }

    public static User socialUserWithId(Long id) {
        return withId(socialUser(DEFAULT_SOCIAL_TYPE, DEFAULT_SOCIAL_ID, DEFAULT_EMAIL, null), id);
    }

    public static User socialUserWithId(Long id, SocialType socialType, String socialId, String email, String profileImage) {
        return withId(socialUser(socialType, socialId, email, profileImage), id);
    }

    public static User activeUser() {
        return activeUser(DEFAULT_SOCIAL_ID, DEFAULT_EMAIL, DEFAULT_PROFILE_IMAGE);
    }

    public static User activeUser(String socialId, String email) {
        return activeUser(socialId, email, DEFAULT_PROFILE_IMAGE);
    }

    public static User activeUserWithId(Long id, String socialId, String email, String profileImage) {
        return withId(activeUser(socialId, email, profileImage), id);
    }

    public static User activeUserWithId(Long id, SocialType socialType, String socialId, String email, String profileImage) {
        User user = socialUserWithId(id, socialType, socialId, email, profileImage);
        user.updateUser(DEFAULT_NICKNAME, email, DEFAULT_DESCRIPTION);
        return user;
    }

    private static User socialUser(SocialType socialType, String socialId, String email, String profileImage) {
        return User.createSocialUser(socialType, socialId, email, profileImage);
    }

    private static User activeUser(String socialId, String email, String profileImage) {
        User user = socialUser(socialId, email, profileImage);
        user.updateUser(DEFAULT_NICKNAME, email, DEFAULT_DESCRIPTION);
        return user;
    }

    private static User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
