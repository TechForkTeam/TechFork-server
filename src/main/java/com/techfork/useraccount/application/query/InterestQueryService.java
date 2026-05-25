package com.techfork.useraccount.application.query;

import com.techfork.useraccount.application.query.result.GetInterestListResult;
import com.techfork.useraccount.application.query.result.GetUserInterestsResult;
import com.techfork.useraccount.application.query.result.UserInterestResult;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserInterestCategoryRepository;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestQueryService {

    private final UserRepository userRepository;
    private final UserInterestCategoryRepository userInterestCategoryRepository;

    public GetInterestListResult getAllInterests() {
        return GetInterestListResult.builder()
                .categories(Arrays.stream(EInterestCategory.values())
                        .map(category -> GetInterestListResult.CategoryResult.builder()
                                .category(category.name())
                                .displayName(category.getDisplayName())
                                .keywords(EInterestKeyword.getKeywordsByCategory(category)
                                        .stream()
                                        .map(keyword -> new GetInterestListResult.KeywordResult(keyword.name(), keyword.getDisplayName()))
                                        .toList())
                                .build())
                        .toList())
                .build();
    }

    public GetUserInterestsResult getUserInterests(GetUserInterestsQuery query) {
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(user.getId());
        List<UserInterestResult> interests = categories.stream()
                .map(category -> UserInterestResult.builder()
                        .category(category.getCategory().name())
                        .keywords(category.getKeywords().stream()
                                .map(keyword -> keyword.getKeyword().name())
                                .toList())
                        .build())
                .toList();

        return GetUserInterestsResult.builder()
                .interests(interests)
                .build();
    }
}
