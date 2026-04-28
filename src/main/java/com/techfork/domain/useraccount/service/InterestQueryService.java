package com.techfork.domain.useraccount.service;

import com.techfork.domain.useraccount.converter.InterestConverter;
import com.techfork.domain.useraccount.dto.InterestListResponse;
import com.techfork.domain.useraccount.dto.UserInterestDto;
import com.techfork.domain.useraccount.dto.UserInterestResponse;
import com.techfork.domain.useraccount.entity.User;
import com.techfork.domain.useraccount.entity.UserInterestCategory;
import com.techfork.domain.useraccount.exception.UserErrorCode;
import com.techfork.domain.useraccount.repository.UserInterestCategoryRepository;
import com.techfork.domain.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestQueryService {

    private final UserRepository userRepository;
    private final UserInterestCategoryRepository userInterestCategoryRepository;
    private final InterestConverter interestConverter;

    public InterestListResponse getAllInterests() {
        return InterestListResponse.builder()
                .categories(interestConverter.toInterestCategoryDtoList())
                .build();
    }

    public UserInterestResponse getUserInterests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserIdWithKeywords(user.getId());
        List<UserInterestDto> userInterestDtos = interestConverter.toUserInterestDtoList(categories);

        return UserInterestResponse.builder()
                .interests(userInterestDtos)
                .build();
    }
}
