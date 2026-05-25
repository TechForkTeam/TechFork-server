package com.techfork.useraccount.converter;

import com.techfork.useraccount.application.command.CompleteOnboardingCommand;
import com.techfork.useraccount.application.command.UpdateAccountProfileCommand;
import com.techfork.useraccount.application.command.UserInterestCommand;
import com.techfork.useraccount.application.query.result.GetAccountProfileResult;
import com.techfork.useraccount.dto.AccountProfileResponse;
import com.techfork.useraccount.dto.OnboardingRequest;
import com.techfork.useraccount.dto.UpdateAccountProfileRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserConverter {

    public CompleteOnboardingCommand toCompleteOnboardingCommand(
            Long userId,
            OnboardingRequest request,
            List<UserInterestCommand> interests
    ) {
        return new CompleteOnboardingCommand(
                userId,
                request.nickname(),
                request.email(),
                request.description(),
                interests
        );
    }

    public UpdateAccountProfileCommand toUpdateAccountProfileCommand(Long userId, UpdateAccountProfileRequest request) {
        return new UpdateAccountProfileCommand(userId, request.nickName(), request.description());
    }

    public AccountProfileResponse toAccountProfileResponse(GetAccountProfileResult result) {
        return AccountProfileResponse.builder()
                .profileImage(result.profileImage())
                .nickName(result.nickName())
                .email(result.email())
                .description(result.description())
                .build();
    }
}
