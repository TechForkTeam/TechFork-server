package com.techfork.domain.user.service;

import com.techfork.domain.user.converter.InterestConverter;
import com.techfork.domain.user.dto.InterestListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestQueryService {

    private final InterestConverter interestConverter;

    public InterestListResponse getAllInterests() {
        return InterestListResponse.builder()
                .categories(interestConverter.toInterestCategoryDtoList())
                .build();
    }
}
