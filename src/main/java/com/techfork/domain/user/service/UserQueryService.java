package com.techfork.domain.user.service;

import com.techfork.domain.user.converter.UserConverter;
import com.techfork.domain.user.dto.AccountProfileResponse;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserConverter userConverter;

    public AccountProfileResponse getAccountProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        log.info("Account profile retrieved for userId: {}", userId);

        return userConverter.toAccountProfileResponse(user);
    }
}
