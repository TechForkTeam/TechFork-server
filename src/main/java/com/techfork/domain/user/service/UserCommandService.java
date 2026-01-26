package com.techfork.domain.user.service;

import com.techfork.domain.user.dto.OnboardingRequest;
import com.techfork.domain.user.dto.SaveInterestRequest;
import com.techfork.domain.user.dto.UpdateUserProfileRequest;
import com.techfork.domain.user.entity.User;
import com.techfork.domain.user.exception.UserErrorCode;
import com.techfork.domain.user.repository.UserRepository;
import com.techfork.global.exception.GeneralException;
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

    public void completeOnboarding(Long userId, @Valid OnboardingRequest request) {
        User user = userRepository.findByIdWithInterestCategories(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        user.updateUser(request.nickname(), request.email(), request.description());

        interestCommandService.saveUserInterests(user, new SaveInterestRequest(request.interests()));
    }

    public void updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.nickName(), request.description());

        log.info("User profile updated for userId: {} - nickName: {}, description: {}",
                userId,
                request.nickName() != null ? "updated" : "unchanged",
                request.description() != null ? "updated" : "unchanged");
    }
}
