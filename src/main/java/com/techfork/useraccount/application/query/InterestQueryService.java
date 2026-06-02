package com.techfork.useraccount.application.query;

import com.techfork.useraccount.application.query.result.GetInterestListResult;
import com.techfork.useraccount.application.query.result.GetUserInterestsResult;
import com.techfork.useraccount.application.query.result.UserInterestResult;
import com.techfork.useraccount.application.reader.UserReader;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.infrastructure.UserInterestCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestQueryService {

    private final UserReader userReader;
    private final UserInterestCategoryRepository userInterestCategoryRepository;

    public GetInterestListResult getAllInterests() {
        return GetInterestListResult.builder()
                .categories(Arrays.stream(EInterestCategory.values())
                        .map(this::toCategoryResult)
                        .toList())
                .build();
    }

    public GetUserInterestsResult getUserInterests(GetUserInterestsQuery query) {
        User user = userReader.getById(query.userId());

        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(user.getId());
        List<UserInterestResult> interests = categories.stream()
                .map(this::toUserInterestResult)
                .toList();

        return GetUserInterestsResult.builder()
                .interests(interests)
                .build();
    }

    private GetInterestListResult.CategoryResult toCategoryResult(EInterestCategory category) {
        return GetInterestListResult.CategoryResult.builder()
                .category(category.name())
                .displayName(category.getDisplayName())
                .keywords(EInterestKeyword.getKeywordsByCategory(category)
                        .stream()
                        .map(this::toKeywordResult)
                        .toList())
                .build();
    }

    private GetInterestListResult.KeywordResult toKeywordResult(EInterestKeyword keyword) {
        return new GetInterestListResult.KeywordResult(keyword.name(), keyword.getDisplayName());
    }

    private UserInterestResult toUserInterestResult(UserInterestCategory category) {
        return UserInterestResult.builder()
                .category(category.getCategory().name())
                .keywords(category.getKeywords().stream()
                        .map(keyword -> keyword.getKeyword().name())
                        .toList())
                .build();
    }
}
