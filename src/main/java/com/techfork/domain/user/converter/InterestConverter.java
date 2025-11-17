package com.techfork.domain.user.converter;

import com.techfork.domain.user.dto.InterestListResponse;
import com.techfork.domain.user.dto.UserInterestDto;
import com.techfork.domain.user.entity.UserInterestCategory;
import com.techfork.domain.user.enums.EInterestCategory;
import com.techfork.domain.user.enums.EInterestKeyword;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class InterestConverter {

    public List<InterestListResponse.Category> toInterestCategoryDtoList() {
        return Arrays.stream(EInterestCategory.values())
                .map(category -> {
                    List<InterestListResponse.Keyword> keywords = EInterestKeyword.getKeywordsByCategory(category)
                            .stream()
                            .map(keyword -> new InterestListResponse.Keyword(keyword.name(), keyword.getDisplayName()))
                            .toList();

                    return InterestListResponse.Category.builder()
                            .category(category.name())
                            .displayName(category.getDisplayName())
                            .keywords(keywords)
                            .build();
                })
                .toList();
    }

    public List<UserInterestDto> toUserInterestDtoList(List<UserInterestCategory> categories) {
        return categories.stream()
                .map(category -> {
                    List<String> keywords = category.getKeywords().stream()
                            .map(keyword -> keyword.getKeyword().name())
                            .toList();

                    return UserInterestDto.builder()
                            .category(category.getCategory().name())
                            .keywords(keywords)
                            .build();
                })
                .toList();
    }
}
