package com.techfork.useraccount.application.query.lookup;

import com.techfork.useraccount.infrastructure.UserInterestCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInterestLookupService {

    private final UserInterestCategoryRepository userInterestCategoryRepository;

    public List<String> getInterestKeywordDisplayNames(Long userId) {
        return userInterestCategoryRepository.findByUserIdWithKeywords(userId)
                .stream()
                .flatMap(category -> category.getKeywords().stream())
                .map(keyword -> keyword.getKeyword().getDisplayName())
                .toList();
    }
}
