package com.techfork.domain.user.service;

import com.techfork.domain.user.converter.InterestConverter;
import com.techfork.domain.user.dto.InterestListResponse;
import com.techfork.domain.user.dto.UserInterestDto;
import com.techfork.domain.user.dto.UserInterestResponse;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.entity.UserInterestCategory;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserInterestCategoryRepository;
import com.techfork.domain.user.repository.UserRepository;
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

        List<UserInterestCategory> categories = userInterestCategoryRepository.findByUserWithKeywords(user);
        List<UserInterestDto> userInterestDtos = interestConverter.toUserInterestDtoList(categories);

        return UserInterestResponse.builder()
                .interests(userInterestDtos)
                .build();
    }
}
