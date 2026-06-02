package com.techfork.useraccount.application.command;

import com.techfork.useraccount.application.command.input.CompleteOnboardingCommand;
import com.techfork.useraccount.application.command.input.UpdateAccountProfileCommand;
import com.techfork.useraccount.application.command.input.WithdrawUserCommand;
import com.techfork.useraccount.domain.User;
import com.techfork.useraccount.domain.exception.UserErrorCode;
import com.techfork.useraccount.infrastructure.UserRepository;
import com.techfork.global.exception.GeneralException;
import com.techfork.global.security.auth.service.UserAuthCacheService;
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

    public void completeOnboarding(CompleteOnboardingCommand command) {
        User user = userRepository.findByIdWithInterestCategories(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        user.updateUser(command.nickname(), command.email(), command.description());

        interestCommandService.saveUserInterests(user, command.interests());

        userAuthCacheService.evict(command.userId());
    }

    public void updateAccountProfile(UpdateAccountProfileCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        user.updateProfile(command.nickName(), command.description());

        log.info("Account profile updated for userId: {} - nickName: {}, description: {}",
                command.userId(),
                command.nickName() != null ? "updated" : "unchanged",
                command.description() != null ? "updated" : "unchanged");
    }

    public void withdrawUser(WithdrawUserCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (user.isWithdrawn()) {
            throw new GeneralException(UserErrorCode.ALREADY_WITHDRAWN);
        }

        user.withdraw();
        userAuthCacheService.evict(command.userId());

        log.info("User withdrawn - status changed to WITHDRAWN and personal data anonymized");
    }
}
