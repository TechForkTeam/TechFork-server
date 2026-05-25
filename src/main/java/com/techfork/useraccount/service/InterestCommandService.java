package com.techfork.useraccount.service;

import com.techfork.useraccount.dto.SaveInterestRequest;
import com.techfork.useraccount.dto.UserInterestDto;
import com.techfork.useraccount.entity.User;
import com.techfork.useraccount.entity.UserInterestCategory;
import com.techfork.useraccount.entity.UserInterestKeyword;
import com.techfork.useraccount.enums.EInterestCategory;
import com.techfork.useraccount.enums.EInterestKeyword;
import com.techfork.useraccount.exception.UserErrorCode;
import com.techfork.useraccount.repository.UserRepository;
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

    public void updateUserInterests(Long userId, SaveInterestRequest request) {
        User user = userRepository.findByIdWithInterestCategories(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        saveUserInterests(user, request);
    }

    void saveUserInterests(User user, SaveInterestRequest request) {
        user.getInterestCategories().clear();
        List<UserInterestCategory> categories = createCategoriesFromRequest(user, request);
        user.getInterestCategories().addAll(categories);

        log.info("Saved {} interest categories for user {}", categories.size(), user.getId());

        personalizationProfileService.generatePersonalizationProfile(user.getId());
    }

    private List<UserInterestCategory> createCategoriesFromRequest(User user, SaveInterestRequest request) {
        return request.interests().stream()
                .map(dto -> createCategoryWithKeywords(user, dto))
                .toList();
    }

    private UserInterestCategory createCategoryWithKeywords(User user, UserInterestDto dto) {
        EInterestCategory category = EInterestCategory.valueOf(dto.category());
        UserInterestCategory userCategory = UserInterestCategory.create(user, category);

        if (dto.keywords() != null && !dto.keywords().isEmpty()) {
            addKeywordsToCategory(userCategory, category, dto.keywords());
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
