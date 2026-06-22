package com.techfork.useraccount.fixture;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.UserInterestKeyword;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;

public final class UserInterestCategoryFixture {

    private UserInterestCategoryFixture() {
    }

    public static UserInterestCategory interestCategory(User user, EInterestCategory category, EInterestKeyword... keywords) {
        UserInterestCategory userInterestCategory = UserInterestCategory.create(user, category);
        for (EInterestKeyword keyword : keywords) {
            userInterestCategory.addKeyword(UserInterestKeyword.create(userInterestCategory, keyword));
        }
        return userInterestCategory;
    }
}
