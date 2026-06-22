package com.techfork.useraccount.fixture;

import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.UserInterestKeyword;
import com.techfork.useraccount.domain.enums.EInterestKeyword;

public final class UserInterestKeywordFixture {

    private UserInterestKeywordFixture() {
    }

    public static UserInterestKeyword interestKeyword(
            UserInterestCategory userInterestCategory,
            EInterestKeyword keyword
    ) {
        return UserInterestKeyword.create(userInterestCategory, keyword);
    }
}
