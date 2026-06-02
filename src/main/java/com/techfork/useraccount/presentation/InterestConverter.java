package com.techfork.useraccount.presentation;

import com.techfork.useraccount.application.command.input.UpdateUserInterestsCommand;
import com.techfork.useraccount.application.command.input.UserInterestCommand;
import com.techfork.useraccount.application.query.result.GetInterestListResult;
import com.techfork.useraccount.application.query.result.GetUserInterestsResult;
import com.techfork.useraccount.presentation.request.SaveInterestRequest;
import com.techfork.useraccount.presentation.request.UserInterestRequest;
import com.techfork.useraccount.presentation.response.InterestListResponse;
import com.techfork.useraccount.presentation.response.UserInterestResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterestConverter {

    public List<UserInterestCommand> toUserInterestCommands(List<UserInterestRequest> interests) {
        return interests.stream()
                .map(interest -> UserInterestCommand.builder()
                        .category(interest.category())
                        .keywords(interest.keywords())
                        .build())
                .toList();
    }

    public UpdateUserInterestsCommand toUpdateUserInterestsCommand(Long userId, SaveInterestRequest request) {
        return new UpdateUserInterestsCommand(userId, toUserInterestCommands(request.interests()));
    }

    public InterestListResponse toInterestListResponse(GetInterestListResult result) {
        return InterestListResponse.builder()
                .categories(result.categories().stream()
                        .map(category -> InterestListResponse.Category.builder()
                                .category(category.category())
                                .displayName(category.displayName())
                                .keywords(category.keywords().stream()
                                        .map(keyword -> new InterestListResponse.Keyword(keyword.keyword(), keyword.displayName()))
                                        .toList())
                                .build())
                        .toList())
                .build();
    }

    public UserInterestResponse toUserInterestResponse(GetUserInterestsResult result) {
        return UserInterestResponse.builder()
                .interests(result.interests().stream()
                        .map(interest -> UserInterestResponse.Interest.builder()
                                .category(interest.category())
                                .keywords(interest.keywords())
                                .build())
                        .toList())
                .build();
    }
}
