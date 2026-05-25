package com.techfork.useraccount.service;

import com.techfork.useraccount.dto.OnboardingRequest;
import com.techfork.useraccount.dto.SaveInterestRequest;
import com.techfork.useraccount.dto.UpdateAccountProfileRequest;
import com.techfork.useraccount.entity.User;
import com.techfork.useraccount.exception.UserErrorCode;
import com.techfork.useraccount.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.security.auth.service.UserAuthCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final InterestCommandService interestCommandService;
    private final UserRepository userRepository;
    private final UserAuthCacheService userAuthCacheService;

    public void completeOnboarding(Long userId, @Valid OnboardingRequest request) {
        User user = userRepository.findByIdWithInterestCategories(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        user.updateUser(request.nickname(), request.email(), request.description());

        interestCommandService.saveUserInterests(user, new SaveInterestRequest(request.interests()));

        userAuthCacheService.evict(userId);
    }

    public void updateAccountProfile(Long userId, UpdateAccountProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.nickName(), request.description());

        log.info("Account profile updated for userId: {} - nickName: {}, description: {}",
                userId,
                request.nickName() != null ? "updated" : "unchanged",
                request.description() != null ? "updated" : "unchanged");
    }

    public void withdrawUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (user.isWithdrawn()) {
            throw new GeneralException(UserErrorCode.ALREADY_WITHDRAWN);
        }

        user.withdraw();
        userAuthCacheService.evict(userId);

        log.info("User withdrawn - status changed to WITHDRAWN and personal data anonymized");
    }
}
