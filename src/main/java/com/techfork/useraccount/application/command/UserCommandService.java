package com.techfork.useraccount.application.command;

import com.techfork.useraccount.application.command.input.CompleteOnboardingCommand;
import com.techfork.useraccount.application.command.input.UpdateAccountProfileCommand;
import com.techfork.useraccount.application.command.input.WithdrawUserCommand;
import com.techfork.useraccount.application.event.OnboardingCompletedEvent;
import com.techfork.useraccount.application.event.UserWithdrawnEvent;
import com.techfork.useraccount.application.reader.UserAggregateReader;
import com.techfork.useraccount.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final InterestCommandService interestCommandService;
    private final UserAggregateReader userAggregateReader;
    private final ApplicationEventPublisher eventPublisher;

    public void completeOnboarding(CompleteOnboardingCommand command) {
        User user = userAggregateReader.getByIdWithInterestCategories(command.userId());

        user.updateUser(command.nickname(), command.email(), command.description());
        interestCommandService.saveUserInterests(user, command.interests());

        eventPublisher.publishEvent(new OnboardingCompletedEvent(command.userId()));
    }

    public void updateAccountProfile(UpdateAccountProfileCommand command) {
        User user = userAggregateReader.getById(command.userId());

        user.updateProfile(command.nickName(), command.description());

        log.info("Account profile updated for userId: {} - nickName: {}, description: {}",
                command.userId(),
                command.nickName() != null ? "updated" : "unchanged",
                command.description() != null ? "updated" : "unchanged");
    }

    public void withdrawUser(WithdrawUserCommand command) {
        User user = userAggregateReader.getById(command.userId());

        user.withdraw();
        eventPublisher.publishEvent(new UserWithdrawnEvent(command.userId()));

        log.info("User withdrawn - status changed to WITHDRAWN and personal data anonymized");
    }
}
