package com.techfork.useraccount.application.command;

import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.UserInterestCategory;
import com.techfork.useraccount.domain.UserInterestKeyword;
import com.techfork.useraccount.domain.enums.EInterestCategory;
import com.techfork.useraccount.domain.enums.EInterestKeyword;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.domain.personalization.service.PersonalizationProfileService;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InterestCommandService {

    private final UserRepository userRepository;
    private final PersonalizationProfileService personalizationProfileService;

    public void updateUserInterests(UpdateUserInterestsCommand command) {
        User user = userRepository.findByIdWithInterestCategories(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        saveUserInterests(user, command.interests());
    }

    void saveUserInterests(User user, List<UserInterestCommand> interests) {
        user.getInterestCategories().clear();
        List<UserInterestCategory> categories = createCategoriesFromRequest(user, interests);
        user.getInterestCategories().addAll(categories);

        log.info("Saved {} interest categories for user {}", categories.size(), user.getId());

        personalizationProfileService.generatePersonalizationProfile(user.getId());
    }

    private List<UserInterestCategory> createCategoriesFromRequest(User user, List<UserInterestCommand> interests) {
        return interests.stream()
                .map(dto -> createCategoryWithKeywords(user, dto))
                .toList();
    }

    private UserInterestCategory createCategoryWithKeywords(User user, UserInterestCommand command) {
        EInterestCategory category = EInterestCategory.valueOf(command.category());
        UserInterestCategory userCategory = UserInterestCategory.create(user, category);

        if (command.keywords() != null && !command.keywords().isEmpty()) {
            addKeywordsToCategory(userCategory, category, command.keywords());
        }

        return userCategory;
    }

    private void addKeywordsToCategory(UserInterestCategory userCategory, EInterestCategory category, List<String> keywordNames) {
        for (String keywordName : keywordNames) {
            EInterestKeyword keyword = EInterestKeyword.valueOf(keywordName);
            validateKeywordCategory(keyword, category);
            UserInterestKeyword userInterestKeyword = UserInterestKeyword.create(userCategory, keyword);
            userCategory.addKeyword(userInterestKeyword);
        }
    }

    private void validateKeywordCategory(EInterestKeyword keyword, EInterestCategory category) {
        if (keyword.getCategory() != category) {
            throw new GeneralException(UserErrorCode.INVALID_INTEREST_KEYWORD);
        }
    }
}
