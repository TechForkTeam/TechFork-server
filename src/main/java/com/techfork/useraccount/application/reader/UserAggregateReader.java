package com.techfork.useraccount.application.reader;

import com.techfork.global.exception.GeneralException;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAggregateReader {

    private final UserRepository userRepository;

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    public User getByIdWithInterestCategories(Long userId) {
        return userRepository.findByIdWithInterestCategories(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }
}
