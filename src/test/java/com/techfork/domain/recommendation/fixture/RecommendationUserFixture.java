package com.techfork.domain.recommendation.fixture;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.enums.SocialType;

public final class RecommendationUserFixture {

    private RecommendationUserFixture() {
    }

    public static User user(String socialId, String email) {
        return User.createSocialUser(
                SocialType.KAKAO,
                socialId,
                email,
                "profile.jpg"
        );
    }

    public static User activeUser(String socialId, String email) {
        User user = user(socialId, email);
        user.updateUser("테스트유저", email, "백엔드 개발자입니다.");
        return user;
    }
}
